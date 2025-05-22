package com.flab.tiple.ticket.waiting.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.message.TipleRedisKey;
import com.flab.tiple.global.util.RedisKeyUtils;
import com.flab.tiple.member.exception.MemberNotMatchException;
import com.flab.tiple.ticket.waiting.domain.TicketWaiting;
import com.flab.tiple.ticket.waiting.domain.TicketWaitingRedis;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingInfoResponseDto;
import com.flab.tiple.ticket.waiting.enums.TicketWaitingStatus;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingNotCancelableException;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingNotFoundException;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TicketWaitingServiceImpl implements TicketWaitingService {

	// Redis 관련 의존성 추가
	private final TicketWaitingRedisRepository ticketWaitingRedisRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final RedisKeyUtils redisKeyUtils;
	private final int timeout = 10;

	private static final int CACHE_TTL_MINUTES = 5; // 캐시 TTL
	private static final int MAX_ITEMS_FOR_RUNTIME_SORT = 100; // 런타임 정렬 임계값

	// Redis 키 상수
	private static final String WAITING_KEY_PREFIX = TipleRedisKey.TICKET_RESERVATION_KEY.getKey();

	@Override
	@Transactional
	public TicketWaitingCancelResponseDto cancelWaiting(Long waitingId, Long memberId) {
		// 1. Redis에서 대기 정보 조회
		TicketWaitingRedis waitingRedis = findWaitingRedisInfo(waitingId, memberId);
		// 2. 정보 검증
		validateWaitingCancellation(waitingRedis);
		// 3. Redis에서 대기 정보 삭제
		return processWaitingCancellationWithConcurrency(waitingRedis);

	}

	@Override
	public List<TicketWaitingInfoResponseDto> getMemberWaitingList(Long memberId) {
		// 1. 캐시된 결과 확인
		String cacheKey = redisKeyUtils.generateMemberWaitingCacheKey(memberId);
		List<TicketWaitingInfoResponseDto> cachedResult = getCachedWaitingList(cacheKey);

		if (cachedResult != null) {
			log.debug("캐시된 대기 목록 반환 for memberId: {}", memberId);
			return cachedResult;
		}

		// 2. Redis에서 회원의 모든 대기 정보 조회
		List<TicketWaitingInfoResponseDto> waitingList = getMemberWaitingListFromRedis(memberId);

		// 3. 성능을 고려한 정렬 처리
		List<TicketWaitingInfoResponseDto> sortedList = optimizedSort(waitingList);

		// 4. 결과 캐싱 (비동기로 처리)
		cacheWaitingListAsync(cacheKey, sortedList);

		return sortedList;
	}

	/**
	 * 대기 목록 캐싱 (비동기)
	 */
	private void cacheWaitingListAsync(String cacheKey, List<TicketWaitingInfoResponseDto> waitingList) {
		try {
			redisTemplate.opsForValue().set(cacheKey, waitingList, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
		} catch (Exception e) {
			log.warn("캐시 저장 실패: {}", e.getMessage());
		}
	}

	/**
	 * 대기 목록 크기에 따른 최적화된 정렬
	 */
	private List<TicketWaitingInfoResponseDto> optimizedSort(List<TicketWaitingInfoResponseDto> waitingList) {
		// 소량의 데이터인 경우 런타임 정렬
		if (waitingList.size() <= MAX_ITEMS_FOR_RUNTIME_SORT) {
			return waitingList.stream()
				.sorted(Comparator.comparing(TicketWaitingInfoResponseDto::getWaitingNumber))
				.collect(Collectors.toList());
		}

		// 대량의 데이터인 경우 Redis Sorted Set 활용 권장
		log.warn("대량의 대기 목록 정렬 감지 ({}개). Redis Sorted Set 사용을 고려해주세요.", waitingList.size());

		// 임시로 런타임 정렬 수행하되, 향후 Sorted Set으로 마이그레이션 권장
		return waitingList.parallelStream()
			.sorted(Comparator.comparing(TicketWaitingInfoResponseDto::getWaitingNumber))
			.collect(Collectors.toList());
	}


	/**
	 * 캐시된 대기 목록 조회
	 * @SuppressWarnings: 제네릭 타입 캐스팅 시 컴파일러 경고 억제
	 */
	@SuppressWarnings("unchecked")
	private List<TicketWaitingInfoResponseDto> getCachedWaitingList(String cacheKey) {
		try {
			return (List<TicketWaitingInfoResponseDto>) redisTemplate.opsForValue().get(cacheKey);
		} catch (Exception e) {
			log.warn("캐시 조회 실패: {}", e.getMessage());
			return null;
		}
	}

	// Redis에서 회원의 대기 목록 조회
	private List<TicketWaitingInfoResponseDto> getMemberWaitingListFromRedis(Long memberId) {
		// 키 패턴을 사용하여 회원의 모든 대기 정보를 조회
		// 주의: 실제 환경에서는 KEYS 명령 대신 SCAN을 사용하는 것이 좋음
		String pattern = WAITING_KEY_PREFIX + "*:" + memberId;
		Set<String> keys = redisTemplate.keys(pattern);

		if (keys == null || keys.isEmpty()) {
			return new ArrayList<>();
		}

		return keys.stream()
			.map(key -> {
				try {
					TicketWaitingRedis waiting = ticketWaitingRedisRepository.findById(key)
						.orElse(null);
					if (waiting == null) return null;

					return TicketWaitingInfoResponseDto.builder()
						.concertId(waiting.getConcertId())
						.waitingNumber(waiting.getWaitingNumber())
						.status(waiting.getStatus())
						.build();
				} catch (Exception e) {
					log.error("Redis 대기 정보 변환 중 오류: {}", e.getMessage(), e);
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	// Redis에서 대기 정보 조회
	private TicketWaitingRedis findWaitingRedisInfo(Long waitingId, Long memberId) {
		// 대기 ID로 Redis 키 생성
		String redisKey = WAITING_KEY_PREFIX + waitingId;

		// Redis에서 대기 정보 조회
		TicketWaitingRedis waiting = ticketWaitingRedisRepository.findById(redisKey)
			.orElseThrow(() -> new TicketWaitingNotFoundException(
				ErrorCode.TICKET_WAITING_NOT_FOUND,
				ErrorCode.TICKET_WAITING_NOT_FOUND.getDescription()
			));

		// 회원 ID 일치 여부 확인
		if (!waiting.getMemberId().equals(memberId)) {
			throw new MemberNotMatchException(
				ErrorCode.MEMBER_NOT_MATCH,
				ErrorCode.MEMBER_NOT_MATCH.getDescription()
			);
		}

		return waiting;
	}

	// Redis 대기 취소 검증
	private void validateWaitingCancellation(TicketWaitingRedis waiting) {
		// 대기 상태 검증
		if (!waiting.getStatus().equals(TicketWaitingStatus.WAITING.toString())) {
			throw new TicketWaitingNotCancelableException(
				ErrorCode.TICKET_WAITING_NOT_CANCELABLE,
				ErrorCode.TICKET_WAITING_NOT_CANCELABLE.getDescription()
			);
		}
	}

	// Redis 대기 취소 처리
	/**
	 * 동시성을 고려한 대기 취소 처리
	 */
	private TicketWaitingCancelResponseDto processWaitingCancellationWithConcurrency(TicketWaitingRedis waiting) {
		String lockKey = redisKeyUtils.generateLockKey(waiting.getId());
		String lockValue = String.valueOf(System.currentTimeMillis());

		try {
			// 분산 락 획득 시도 (30초 타임아웃, 10초 유지)
			Boolean lockAcquired = redisTemplate.opsForValue()
				.setIfAbsent(lockKey, lockValue, timeout, TimeUnit.SECONDS);

			if (lockAcquired == null || !lockAcquired) {
				throw new TicketWaitingNotCancelableException(
					ErrorCode.TICKET_WAITING_CONCURRENT_ACCESS,
					ErrorCode.TICKET_WAITING_CONCURRENT_ACCESS.getDescription()
				);
			}

			// 최신 상태 재조회하여 동시성 문제 방지
			String redisKey = waiting.getId();
			TicketWaitingRedis latestWaiting = ticketWaitingRedisRepository.findById(redisKey)
				.orElseThrow(() -> new TicketWaitingNotFoundException(
					ErrorCode.TICKET_WAITING_NOT_FOUND,
					ErrorCode.TICKET_WAITING_NOT_FOUND.getDescription()
				));

			// 재검증
			validateWaitingCancellation(latestWaiting);

			// 원자적 상태 변경
			return executeAtomicStatusChange(latestWaiting);

		} finally {
			releaseLockSafely(lockKey, lockValue);
		}
	}

	/**
	 * 루아 스크립트 사용 이유:
	 * redisTemplate의 경우 원자적이지 않음 ->
	 * 예를들어  redisTemplate.opsForHash().put(key, "status", "CANCELED"); 이 명령어 실행 사이에 다른 요청이 끼어들 수 있음!
	 * lua로 실행시 전체가 하나의 원자적 연산으로 실행됨
	 *
	 * 원자성: Lua는 전체 스크립트가 원자적으로 실행
	 * 네트워크 횟수: Lua는 1번, RedisTemplate는 여러 번
	 * 동시성: Lua는 중간에 다른 요청이 끼어들 수 없음
	 */

	private void releaseLockSafely(String lockKey, String lockValue) {
		String script =
			"if redis.call('GET', KEYS[1]) == ARGV[1] then " +
				"  return redis.call('DEL', KEYS[1]) " +
				"else " +
				"  return 0 " +
				"end";

		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptText(script);
		redisScript.setResultType(Long.class);

		redisTemplate.execute(redisScript,
			Collections.singletonList(lockKey),
			lockValue
		);
	}

	/**
	 * 원자적 상태 변경 실행
	 */
	private TicketWaitingCancelResponseDto executeAtomicStatusChange(TicketWaitingRedis waiting) {
		// 상태 변경 전 조건 체크하는 Lua 스크립트 실행
		String script =
			"local current = redis.call('HGET', KEYS[1], 'status') " +
				"if current == ARGV[1] then " +
				"  redis.call('HSET', KEYS[1], 'status', ARGV[2]) " +
				"  return 1 " +
				"else " +
				"  return 0 " +
				"end";

		// RedisScript 객체 사용으로 수정
		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptText(script);
		redisScript.setResultType(Long.class);

		Long result = redisTemplate.execute(redisScript,
			Collections.singletonList(waiting.getId()),
			TicketWaitingStatus.WAITING.toString(),
			TicketWaitingStatus.CANCELED.toString()
		);

		if (result == null || result == 0) {
			throw new TicketWaitingNotCancelableException(
				ErrorCode.TICKET_WAITING_NOT_CANCELABLE,
				"대기 상태가 변경되어 취소할 수 없습니다."
			);
		}

		// 캐시 무효화
		invalidateMemberWaitingCache(waiting.getMemberId());

		// 응답 DTO 생성
		return TicketWaitingCancelResponseDto.builder()
			.id(redisKeyUtils.extractIdFromRedisKey(waiting.getId()))
			.status(TicketWaitingStatus.CANCELED)
			.deletedAt(LocalDateTime.now().toString())
			.build();
	}

	/**
	 * 회원 대기 목록 캐시 무효화
	 */
	private void invalidateMemberWaitingCache(Long memberId) {
		String cacheKey = redisKeyUtils.generateMemberWaitingCacheKey(memberId);
		try {
			redisTemplate.delete(cacheKey);
		} catch (Exception e) {
			log.warn("캐시 무효화 실패: {}", e.getMessage());
		}
	}


}
