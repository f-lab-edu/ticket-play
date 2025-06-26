package com.flab.tiple.reservation.application.notification.handler;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.reservation.application.dto.response.FailedNotificationDto;
import com.flab.tiple.reservation.application.event.reservation.ReservationNotificationEvent;
import com.flab.tiple.reservation.application.event.reservation.SeatAvailableNotificationEvent;
import com.flab.tiple.reservation.application.event.reservation.WaitingNumberNotificationEvent;
import com.flab.tiple.reservation.application.notification.enums.NotificationStatus;
import com.flab.tiple.reservation.infrastructure.message.producer.KafkaNotificationProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventHandler {
	private final KafkaNotificationProducer kafkaNotificationProducer;
	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;

	// DLQ 관련 상수
	private final String FAILED_NOTIFICATION_ZSET = "failed_notifications";
	private final String NOTIFICATION_DLQ = "notification_dlq";
	private final int MAX_DLQ_SIZE = 1000;

	// 예약 상태 변경 알림
	@Async("notificationTaskExecutor")
	@EventListener
	@Retryable(
		value = {Exception.class},
		maxAttemptsExpression = "#{${spring.retryable.maxAttempts}}",
		backoff = @Backoff(
			delayExpression = "#{${spring.retryable.backoff.delay}}",
			multiplierExpression = "#{${spring.retryable.backoff.multiplier}}"
		)
	)
	public void handleReservationNotification(ReservationNotificationEvent event) {
		try {
			kafkaNotificationProducer.notifyReservationStatusChanged(
				event.getMemberId(),
				event.getReservationId(),
				event.getStatus()
			);

		} catch (Exception e) {
			log.error("예약 상태 알림 전송 실패: memberId={}, reservationId={}, error={}",
				event.getMemberId(), event.getReservationId(), e.getMessage(), e);
			throw e;
		}
	}

	// 대기번호 알림
	@Async("notificationTaskExecutor")
	@EventListener
	@Retryable(
		value = {Exception.class},
		maxAttemptsExpression = "#{${spring.retryable.maxAttempts}}",
		backoff = @Backoff(
			delayExpression = "#{${spring.retryable.backoff.delay}}",
			multiplierExpression = "#{${spring.retryable.backoff.multiplier}}"
		)
	)
	public void handleWaitingNumberNotification(WaitingNumberNotificationEvent event) {
		try {
			kafkaNotificationProducer.notifyWaitingNumber(
				event.getMemberId(),
				event.getConcertId(),
				event.getWaitingNumber()
			);

			log.info("대기번호 알림 전송 완료: memberId={}, concertId={}, waitingNumber={}",
				event.getMemberId(), event.getConcertId(), event.getWaitingNumber());
		} catch (Exception e) {
			log.error("대기번호 알림 전송 실패: memberId={}, concertId={}, waitingNumber={}, error={}",
				event.getMemberId(), event.getConcertId(), event.getWaitingNumber(), e.getMessage(), e);
			throw e;
		}
	}

	// 좌석 사용가능 알림
	@Async("notificationTaskExecutor")
	@EventListener
	@Retryable(
		value = {Exception.class},
		maxAttemptsExpression = "#{${spring.retryable.maxAttempts}}",
		backoff = @Backoff(
			delayExpression = "#{${spring.retryable.backoff.delay}}",
			multiplierExpression = "#{${spring.retryable.backoff.multiplier}}"
		)
	)
	public void handleSeatAvailableNotification(SeatAvailableNotificationEvent event) {
		try {
			kafkaNotificationProducer.notifySeatAvailable(
				event.getMemberId(),
				event.getSeatId(),
				event.getConcertId(),
				event.getMessage()
			);

			log.info("좌석 사용가능 알림 전송 완료: memberId={}, seatId={}, concertId={}",
				event.getMemberId(), event.getSeatId(), event.getConcertId());
		} catch (Exception e) {
			log.error("좌석 사용가능 알림 전송 실패: memberId={}, seatId={}, concertId={}, error={}",
				event.getMemberId(), event.getSeatId(), event.getConcertId(), e.getMessage(), e);
			throw e;
		}
	}

	// 복구 메서드들 (기존과 동일)
	@Recover
	public void recoverReservationNotification(Exception ex, ReservationNotificationEvent event) {
		log.error("예약 알림 전송 최종 실패 (3회 재시도 후): memberId={}, reservationId={}",
			event.getMemberId(), event.getReservationId(), ex);

		try {
			String eventJson = objectMapper.writeValueAsString(event);
			saveFailedNotification("reservation", event.getMemberId(), eventJson, ex.getMessage());
		} catch (Exception e) {
			log.error("실패한 예약 알림 저장 실패", e);
			moveToDeadLetterQueue("reservation", event.getMemberId(), ex.getMessage());
		}
	}

	@Recover
	public void recoverWaitingNumberNotification(Exception ex, WaitingNumberNotificationEvent event) {
		log.error("대기번호 알림 전송 최종 실패 (3회 재시도 후): memberId={}, concertId={}",
			event.getMemberId(), event.getConcertId(), ex);

		try {
			String eventJson = objectMapper.writeValueAsString(event);
			saveFailedNotification("waiting", event.getMemberId(), eventJson, ex.getMessage());
		} catch (Exception e) {
			log.error("실패한 대기번호 알림 저장 실패", e);
			moveToDeadLetterQueue("waiting", event.getMemberId(), ex.getMessage());
		}
	}

	@Recover
	public void recoverSeatAvailableNotification(Exception ex, SeatAvailableNotificationEvent event) {
		log.error("좌석 알림 전송 최종 실패 (3회 재시도 후): memberId={}, seatId={}",
			event.getMemberId(), event.getSeatId(), ex);

		try {
			String eventJson = objectMapper.writeValueAsString(event);
			saveFailedNotification("seat", event.getMemberId(), eventJson, ex.getMessage());
		} catch (Exception e) {
			log.error("실패한 좌석 알림 저장 실패", e);
			moveToDeadLetterQueue("seat", event.getMemberId(), ex.getMessage());
		}
	}

	// 기존 DLQ 메서드들 그대로 유지
	private void saveFailedNotification(String type, Long memberId, String eventJson, String errorMessage) {
		try {
			String failedId = UUID.randomUUID().toString();
			long currentTime = System.currentTimeMillis();
			long nextRetryTime = currentTime + (5 * 60 * 1000);

			FailedNotificationDto failedData = FailedNotificationDto.builder()
				.id(failedId)
				.type(type)
				.memberId(memberId)
				.originalEventData(eventJson)
				.attemptCount(0)
				.createdAt(currentTime)
				.nextRetryAt(nextRetryTime)
				.status(NotificationStatus.PENDING)
				.build();

			String failedDataJson = objectMapper.writeValueAsString(failedData);
			redisTemplate.opsForZSet().add(FAILED_NOTIFICATION_ZSET, failedDataJson, nextRetryTime);

		} catch (Exception e) {
			log.error("실패한 알림 저장 중 오류", e);
			moveToDeadLetterQueue(type, memberId, errorMessage);
		}
	}

	private void moveToDeadLetterQueue(String type, Long memberId, String errorMessage) {
		try {
			Map<String, Object> dlqData = Map.of(
				"type", type,
				"memberId", memberId,
				"errorMessage", errorMessage,
				"timestamp", System.currentTimeMillis()
			);

			String dlqDataJson = objectMapper.writeValueAsString(dlqData);
			redisTemplate.opsForList().leftPush(NOTIFICATION_DLQ, dlqDataJson);
			redisTemplate.opsForList().trim(NOTIFICATION_DLQ, 0, MAX_DLQ_SIZE - 1);

			log.warn("알림 DLQ 이동: type={}, memberId={}", type, memberId);
		} catch (Exception e) {
			log.error("DLQ 이동 실패", e);
		}
	}
}