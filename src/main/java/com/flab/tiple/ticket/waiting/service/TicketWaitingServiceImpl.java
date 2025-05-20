package com.flab.tiple.ticket.waiting.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.message.TipleRedisKey;
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
		return processWaitingCancellationRedis(waitingRedis);

	}

	@Override
	public List<TicketWaitingInfoResponseDto> getMemberWaitingList(Long memberId) {
		// Redis에서만 회원의 모든 대기 정보 조회
		List<TicketWaitingInfoResponseDto> redisWaitingList = getMemberWaitingListFromRedis(memberId);

		// 대기 번호 기준으로 정렬
		return redisWaitingList.stream()
			.sorted(Comparator.comparing(TicketWaitingInfoResponseDto::getWaitingNumber))
			.collect(Collectors.toList());
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
	private TicketWaitingCancelResponseDto processWaitingCancellationRedis(TicketWaitingRedis waiting) {
		// 상태 변경
		waiting.changeStatus(TicketWaitingStatus.CANCELED.toString());

		// Redis에 저장
		ticketWaitingRedisRepository.save(waiting);

		// 응답 DTO 생성
		return TicketWaitingCancelResponseDto.builder()
			.id(Long.parseLong(waiting.getId().split(":")[1])) // Redis 키에서 ID 추출
			.status(TicketWaitingStatus.CANCELED)
			.deletedAt(LocalDateTime.now().toString())
			.build();
	}

	private TicketWaitingInfoResponseDto TicketWaitingResponseToDto(TicketWaiting ticketWaiting) {
		Concert ticketConcert = ticketWaiting.getConcert();
		return TicketWaitingInfoResponseDto.builder()
			.concertId(ticketConcert.getId())
			.waitingNumber(ticketWaiting.getWaitingNumber())
			.status(ticketWaiting.getStatus().name())
			.build();
	}

}
