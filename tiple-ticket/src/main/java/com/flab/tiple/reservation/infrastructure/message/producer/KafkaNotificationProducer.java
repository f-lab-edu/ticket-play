package com.flab.tiple.reservation.infrastructure.message.producer;

import com.flab.tiple.reservation.domain.model.enums.TicketReservationStatus;
import com.flab.tiple.reservation.infrastructure.message.event.KafkaNotificationEvent;
import com.flab.tiple.reservation.infrastructure.message.event.ReservationStatusKafkaEvent;
import com.flab.tiple.reservation.infrastructure.message.event.SeatAvailableKafkaEvent;
import com.flab.tiple.reservation.infrastructure.message.event.WaitingNumberKafkaEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationProducer {
	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Value("${app.kafka.topics.notification}")
	private String notificationTopic;

	/**
	 * 좌석 이용 가능 알림을 Kafka로 발행
	 */
	public void notifySeatAvailable(Long memberId, Long seatId, Long concertId, String message) {
		SeatAvailableKafkaEvent event = SeatAvailableKafkaEvent.of(memberId, seatId, concertId, message);
		sendNotificationEvent(event);

		log.info("좌석 이용 가능 알림 이벤트 발행: memberId={}, seatId={}, concertId={}",
			memberId, seatId, concertId);
	}

	/**
	 * 예약 상태 변경 알림을 Kafka로 발행
	 */
	public void notifyReservationStatusChanged(Long memberId, Long reservationId, TicketReservationStatus status) {
		ReservationStatusKafkaEvent event = ReservationStatusKafkaEvent.of(memberId, reservationId, status);
		sendNotificationEvent(event);

		log.info("예약 상태 변경 알림 이벤트 발행: memberId={}, reservationId={}, status={}",
			memberId, reservationId, status);
	}

	/**
	 * 대기 번호 알림을 Kafka로 발행
	 */
	public void notifyWaitingNumber(Long memberId, Long concertId, int waitingNumber) {
		WaitingNumberKafkaEvent event = WaitingNumberKafkaEvent.of(memberId, concertId, waitingNumber);
		sendNotificationEvent(event);

		log.info("대기 번호 알림 이벤트 발행: memberId={}, concertId={}, waitingNumber={}",
			memberId, concertId, waitingNumber);
	}

	private void sendNotificationEvent(KafkaNotificationEvent event) {
		try {
			// Spring Boot 3.x에서는 CompletableFuture 사용
			CompletableFuture<SendResult<String, Object>> future =
				kafkaTemplate.send(notificationTopic, event.getMemberId().toString(), event);

			future.whenComplete((result, ex) -> {
				if (ex == null) {
					log.debug("알림 이벤트 전송 성공: eventId={}, topic={}, partition={}, offset={}",
						event.getEventId(), notificationTopic,
						result.getRecordMetadata().partition(),
						result.getRecordMetadata().offset());
				} else {
					log.error("알림 이벤트 전송 실패: eventId={}, error={}",
						event.getEventId(), ex.getMessage(), ex);
				}
			});

		} catch (Exception e) {
			log.error("Kafka 이벤트 발행 중 예외 발생: eventId={}, error={}",
				event.getEventId(), e.getMessage(), e);
		}
	}
}