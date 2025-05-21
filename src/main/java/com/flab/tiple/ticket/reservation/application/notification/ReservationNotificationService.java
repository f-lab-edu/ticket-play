package com.flab.tiple.ticket.reservation.application.notification;

import org.springframework.stereotype.Service;

import com.flab.tiple.global.infrastructure.notification.dto.NotificationDto;
import com.flab.tiple.global.infrastructure.notification.sse.SseEmitterService;
import com.flab.tiple.global.message.TipleMessage;
import com.flab.tiple.ticket.reservation.domain.model.enums.TicketReservationStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationNotificationService {
	private final SseEmitterService sseEmitterService;

	/**
	 * 특정 회원에게 좌석 이용 가능 알림을 전송합니다.
	 */
	public void notifySeatAvailable(Long memberId, Long seatId, Long concertId, String message) {
		NotificationDto notification = NotificationDto.builder()
			.message(message)
			.seatId(seatId)
			.concertId(concertId)
			.build();

		sseEmitterService.sendToMember(
			memberId,
			notification,
			TipleMessage.RESERVATION_SSE_EVENT_NAME.getMessage()
		);

		log.info("좌석 이용 가능 알림 전송: memberId={}, seatId={}, concertId={}",
			memberId, seatId, concertId);
	}

	/**
	 * 예약 상태 변경 알림을 전송합니다.
	 */
	public void notifyReservationStatusChanged(Long memberId, Long reservationId, TicketReservationStatus status) {
		String message = String.format("예약 #%d의 상태가 [%s]로 변경되었습니다.",
			reservationId, status.name());

		NotificationDto notification = NotificationDto.builder()
			.message(message)
			.build();

		sseEmitterService.sendToMember(
			memberId,
			notification,
			"RESERVATION_STATUS_CHANGED"
		);

		log.info("예약 상태 변경 알림 전송: memberId={}, reservationId={}, status={}",
			memberId, reservationId, status);
	}

	/**
	 * 대기 번호 알림을 전송합니다.
	 */
	public void notifyWaitingNumber(Long memberId, Long concertId, int waitingNumber) {
		String message = String.format("콘서트 #%d의 대기 번호는 %d번입니다.",
			concertId, waitingNumber);

		NotificationDto notification = NotificationDto.builder()
			.message(message)
			.concertId(concertId)
			.build();

		sseEmitterService.sendToMember(
			memberId,
			notification,
			"WAITING_NUMBER_ASSIGNED"
		);

		log.info("대기 번호 알림 전송: memberId={}, concertId={}, waitingNumber={}",
			memberId, concertId, waitingNumber);
	}
}
