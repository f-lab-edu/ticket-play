package com.flab.tiple.waiting.domain.service;

import java.time.Duration;
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
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flab.tiple.auth.exception.MemberNotMatchException;
import com.flab.tiple.exception.ErrorCode;
import com.flab.tiple.messaging.redis.TipleRedisKey;
import com.flab.tiple.util.RedisKeyUtils;
import com.flab.tiple.waiting.application.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.waiting.application.dto.response.TicketWaitingInfoResponseDto;
import com.flab.tiple.waiting.domain.exception.TicketWaitingNotCancelableException;
import com.flab.tiple.waiting.domain.exception.TicketWaitingNotFoundException;
import com.flab.tiple.waiting.domain.model.TicketWaitingRedis;
import com.flab.tiple.waiting.domain.model.TicketWaitingStatus;
import com.flab.tiple.waiting.domain.repository.TicketWaitingRedisRepository;

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

	// Redis 키 상수
	private static final String WAITING_KEY_PREFIX = TipleRedisKey.TICKET_RESERVATION_KEY.getKey();
	// Sorted Set 키 상수 추가
	private static final String MEMBER_WAITING_SORTED_SET_PREFIX = "member:waiting:sorted:";

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

		// 1-1. 캐시된 결과가 있으면 캐시된 결과 return
		if (cachedResult != null) {
			return cachedResult;
		}

		// 2. Redis에서 회원의 모든 대기 정보 조회
		List<TicketWaitingInfoResponseDto> waitingList = getMemberWaitingListFromRedis(memberId);

		// 3. 비동기 정렬
		List<TicketWaitingInfoResponseDto> sortedList = optimizedSort(waitingList);

		// 4. 결과 캐싱 (비동기로 처리)
		cacheWaitingList(cacheKey, sortedList);

		return sortedList;
	}

	/**
	 * 대기 목록 캐싱 (비동기)
	 */
	private void cacheWaitingList(String cacheKey, List<TicketWaitingInfoResponseDto> waitingList) {
		try {
			redisTemplate.opsForValue().set(cacheKey, waitingList, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
		} catch (Exception e) {
			log.warn("캐시 저장 실패: {}", e.getMessage());
		}
	}

	/**
	 * parallelStream이로 비동기성 정렬
	 */
	private List<TicketWaitingInfoResponseDto> optimizedSort(List<TicketWaitingInfoResponseDto> waitingList) {
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
		// 1. 캐시된 결과 확인
		String cacheKey = redisKeyUtils.generateMemberWaitingCacheKey(memberId);
		List<TicketWaitingInfoResponseDto> cachedResult = getCachedWaitingList(cacheKey);

		if (cachedResult != null) {
			log.debug("캐시된 대기 목록 반환 for memberId: {}", memberId);
			return cachedResult;
		}

		// 2. Sorted Set에서 정렬된 대기 목록 조회
		List<TicketWaitingInfoResponseDto> waitingList = getMemberWaitingListFromSortedSet(memberId);

		// 3. 결과 캐싱 (비동기로 처리)
		cacheWaitingList(cacheKey, waitingList);

		return waitingList;
	}

	/**
	 * Sorted Set에서 회원의 대기 목록 조회 (이미 정렬됨)
	 */
	private List<TicketWaitingInfoResponseDto> getMemberWaitingListFromSortedSet(Long memberId) {
		String sortedSetKey = MEMBER_WAITING_SORTED_SET_PREFIX + memberId;

		try {
			// ZRANGE로 waitingNumber 순으로 정렬된 데이터 조회
			// ZSetOperations는 **score(점수)**를 가지고 자동 정렬되는 Redis Sorted Set
			Set<ZSetOperations.TypedTuple<Object>> waitingData = redisTemplate.opsForZSet()
				.rangeWithScores(sortedSetKey, 0, -1);

			if (waitingData == null || waitingData.isEmpty()) {
				return new ArrayList<>();
			}

			return waitingData.stream()
				.map(tuple -> {
					try {
						String waitingRedisKey = (String) tuple.getValue();
						Double waitingNumber = tuple.getScore();

						// 개별 대기 정보 조회
						TicketWaitingRedis waiting = ticketWaitingRedisRepository
							.findById(waitingRedisKey)
							.orElse(null);

						if (waiting == null) {
							// Sorted Set에는 있지만 실제 데이터가 없는 경우 정리
							cleanupOrphanedSortedSetEntry(sortedSetKey, waitingRedisKey);
							return null;
						}

						return TicketWaitingInfoResponseDto.builder()
							.concertId(waiting.getConcertId())
							.waitingNumber(convertScoreToWaitingNumber(waitingNumber))
							.status(waiting.getStatus().toString())
							.build();

					} catch (Exception e) {
						log.error("Sorted Set 대기 정보 변환 중 오류: {}", e.getMessage(), e);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("Sorted Set 조회 실패, 기존 방식으로 폴백: {}", e.getMessage());
			return getMemberWaitingListFromRedis(memberId);
		}
	}

	public Integer convertScoreToWaitingNumber(Double score) {
		return (int) Math.round(score);
	}

	/**
	 * 대기 등록 시 Sorted Set에도 추가
	 */
	@Transactional
	public void registerWaitingToSortedSet(Long memberId, String waitingRedisKey, Long waitingNumber) {
		String sortedSetKey = redisKeyUtils.generateMemberWaitingSortedSetKey(memberId);

		try {
			// waitingNumber를 score로 사용하여 Sorted Set에 추가
			redisTemplate.opsForZSet().add(sortedSetKey, waitingRedisKey, waitingNumber);

			// Sorted Set에 TTL 설정 (옵션)
			redisTemplate.expire(sortedSetKey, Duration.ofDays(7));

		} catch (Exception e) {
			log.error("Sorted Set 등록 실패: {}", e.getMessage(), e);
		}
	}


	/**
	 * 고아 상태의 Sorted Set 엔트리 정리
	 */
	private void cleanupOrphanedSortedSetEntry(String sortedSetKey, String waitingRedisKey) {
		try {
			redisTemplate.opsForZSet().remove(sortedSetKey, waitingRedisKey);
			log.debug("고아 상태의 Sorted Set 엔트리 제거: {}", waitingRedisKey);
		} catch (Exception e) {
			log.warn("고아 엔트리 정리 실패: {}", e.getMessage());
		}
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
		TicketWaitingRedis.validateCancel(waiting);
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
	 * 원자성이란? "모든 작업이 완전히 성공하거나 완전히 실패하는" 특성
	 * 네트워크 횟수: Lua는 1번, RedisTemplate는 여러 번
	 * 동시성: Lua는 중간에 다른 요청이 끼어들 수 없음
	 *
	 * Lua 스크립트가 Redis 서버에서 실행되는 과정:
	 * 1. 클라이언트가 스크립트를 Redis 서버로 전송
	 * 2. Redis 서버가 Lua 인터프리터로 스크립트 실행
	 * 3. 스크립트 내의 모든 Redis 명령어가 순차적으로 실행
	 * 4. 결과를 클라이언트에게 반환
	 *
	 * Lua 스크립트에서 원자성이 보장되는 이유:
	 1. Redis는 싱글 스레드 이벤트 루프로 동작
	 - 모든 명령어는 순차적으로 실행
	 - 동시 실행이 불가능

	 2. Lua 스크립트는 하나의 "명령어"로 취급
	 - 스크립트 전체가 하나의 단위
	 - 중간에 다른 명령어가 끼어들 수 없음
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
			"local current = redis.call('HGET', KEYS[1], 'status') " +  // 1. 현재 상태 조회
				"if current == ARGV[1] then " +                             // 2. 조건 확인
				"  redis.call('HSET', KEYS[1], 'status', ARGV[2]) " +       // 3. 상태 변경
				"  return 1 " +                                             // 4. 성공 반환
				"else " +
				"  return 0 " +                                             // 5. 실패 반환
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
