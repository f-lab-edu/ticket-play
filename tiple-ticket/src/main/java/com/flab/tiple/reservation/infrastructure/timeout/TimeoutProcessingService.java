package com.flab.tiple.reservation.infrastructure.timeout;


/**
 * DLQ (Dead Letter Queue) 개념
 * DLQ란?
 *
 * **"배달 불가능한 편지함"**에서 유래된 개념
 * 처리할 수 없는 메시지들을 저장하는 특별한 저장소
 * 복구 불가능한 실패 데이터를 안전하게 보관
 *
 * DLQ의 목적
 * 데이터 손실 방지 : 실패한 작업도 기록으로 보존 => 완전한 데이터 보존
 * 장애 분석: 실패 원인 파악을 위한 증거 => 근본 원인 분석
 * 수동 복구: 관리자가 나중에 직접 처리 => 유연한 복구 옵션
 * 시스템 안정성: 문제 데이터가 시스템을 막지 않음 => 시스템 보호
 */

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.CircuitBreaker;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.messaging.redis.TipleRedisKey;
import com.flab.tiple.reservation.application.event.reservation.ReservationTimeoutEvent;
import com.flab.tiple.reservation.domain.model.TicketReservationTimeoutData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 장애 시나리오
 * 시나리오 1: 일시적 네트워크 오류
 * processExpiredItem() → Redis 연결 실패
 * ↓ handleFailedItem() → failed_timeouts 저장
 * ↓ 1차 재시도 → 성공
 * 결과: 정상 처리됨 (failed_timeouts에 기록만 남음)
 *
 * 시나리오 2: JSON 파싱 오류 (복구 불가)
 * processExpiredItem() → JSON 파싱 실패
 * ↓ handleFailedItem() → failed_timeouts 저장
 * ↓ 1,2,3차 재시도 → 모두 동일 오류
 * ↓ recover() 자동 호출
 * ↓ moveToDeadLetterQueue() → timeout_dlq 저장
 * 결과: DLQ에서 관리자 수동 처리 필요
 *
 * 시나리오 3: 시스템 전체 장애
 * processExpiredItem() → 예외 발생
 * ↓ handleFailedItem() → Redis 연결 실패
 * ↓ 1,2,3차 재시도 → 모든 Redis 연결 실패
 * ↓ recover() → moveToDeadLetterQueue() 실패
 * 결과: 로그에만 기록, 시스템 복구 후 수동 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TimeoutProcessingService {
	private final RedisTemplate<String, Object> redisTemplate;
	private final ApplicationEventPublisher eventPublisher;
	private final ObjectMapper objectMapper;
	private final int MAX_ATTEMPTS = 5;
	private final int OPEN_TIMEOUT = 10000;
	private final int RESET_TIMEOUT = 30000;

	private final int dlqKeyStart = 0;
	private final int dlqKeyEnd = 1000;

	private final String dlqKey = "timeout_dlq";
	private final String TIMEOUT_ZSET = "reservation_timeouts";
	private final String FAIL_KEY = "failed_timeouts";

	/**
	 * @Retryable + @CircuitBreaker
	 * Step 1: 정상 처리 시도
	 * processExpiredItem(item)
	 *  ─ JSON 파싱
	 *  ─ 예약 상태 확인
	 *  ─ 타임아웃 이벤트 발행
	 *  ─ ZSet에서 제거
	 */
	@Retryable(
		value = {Exception.class},
		maxAttemptsExpression = "#{${spring.retryable.maxAttempts}}",
		backoff = @Backoff(
			delayExpression = "#{${spring.retryable.backoff.delay}}",
			multiplierExpression = "#{${spring.retryable.backoff.multiplier}}"
		)
	)
	@CircuitBreaker(
		maxAttempts = MAX_ATTEMPTS,
		openTimeout = OPEN_TIMEOUT,
		resetTimeout = RESET_TIMEOUT
	)
	public void processExpiredItem(ZSetOperations.TypedTuple<Object> item) throws JsonProcessingException {
		try {
			String value = (String) item.getValue();
			TicketReservationTimeoutData timeoutData = objectMapper.readValue(value, TicketReservationTimeoutData.class);


			// 실제 예약 상태 확인
			String reservationKey = timeoutData.getSeatId() + ":" + timeoutData.getMemberId();
			String fullKey = TipleRedisKey.TICKET_RESERVATION_KEY.getKey() + reservationKey;

			if (Boolean.TRUE.equals(redisTemplate.hasKey(fullKey))) {
				// 타임아웃 이벤트 발행
				ReservationTimeoutEvent timeoutEvent = ReservationTimeoutEvent.builder()
					.reservationId(timeoutData.getReservationId())
					.seatId(timeoutData.getSeatId())
					.memberId(timeoutData.getMemberId())
					.concertId(timeoutData.getConcertId())
					.build();

				eventPublisher.publishEvent(timeoutEvent);
			}

			// 처리된 항목 제거 (원자적 연산)
			redisTemplate.opsForZSet().remove(TIMEOUT_ZSET, value);

		} catch (Exception e) {
			//Step 2: 에러시 실패 저장
			//Step 3: 자동 재시도
			handleFailedItem(item);
			throw e; // Retryable이 동작하도록
		}
	}


	/**
	 * @Recover는 Spring Retry가 @Retryable 메소드의 모든 재시도가 실패한 후 자동으로 실행
	 *
	 * Step 4: 최종 실패 처리
	 * processExpiredItem() 실행
	 * ↓ 예외 발생
	 * ↓ 1차 재시도 (실패)
	 * ↓ 2차 재시도 (실패)
	 * ↓ 3차 재시도 (실패)
	 * ↓ recover() 메소드 자동 호출
	 */
	@Recover
	public void recover(Exception ex, ZSetOperations.TypedTuple<Object> item) {
		log.error("타임아웃 처리 최종 실패, DLQ로 이동: {}", item.getValue(), ex);
		moveToDeadLetterQueue(item);
	}

	/**
	 * Step 2: 에러시 실패 저장(디테일)
	 *  ─ failed_timeouts ZSet에 저장
	 *  ─ timestamp를 score로 사용
	 *  ─ 수동 재처리 가능
	 */
	private void handleFailedItem(ZSetOperations.TypedTuple<Object> item) {
		try {
			// failed_timeouts : ZSet 타입으로 저장 : 처리 실패한 작업
			redisTemplate.opsForZSet().add(FAIL_KEY, item.getValue(), System.currentTimeMillis());
		} catch (Exception e) {
			log.error("실패 항목 저장 실패", e);
		}
	}

	/**
	 * Step 5: DLQ 최종 저장
	 * moveToDeadLetterQueue():
	 * ─ timeout_dlq List에 leftPush
	 * ─ 크기 제한 (0~1000)
	 * ─ 자동 처리 포기
	 */
	private void moveToDeadLetterQueue(ZSetOperations.TypedTuple<Object> item) {
		try {
			// timeout_dlq: List형태 저장 : 복구 불가능한 작업
			redisTemplate.opsForList().leftPush(dlqKey, item.getValue());
			// DLQ 크기 제한
			redisTemplate.opsForList().trim(dlqKey, dlqKeyStart, dlqKeyEnd);
		} catch (Exception e) {
			log.error("DLQ 이동 실패", e);
		}
	}
}
