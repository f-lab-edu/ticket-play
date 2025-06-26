package com.flab.tiple.application;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.flab.tiple.application.dto.NotificationDto;
import com.flab.tiple.domain.repository.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseNotificationUseCase {
	private final NotificationService notificationService;

	public SseEmitter subscribeToNotifications(Long memberId) {
		log.info("SSE 구독 요청 - 회원 ID: {}", memberId);
		return notificationService.subscribe(memberId);
	}

	public void sendTicketReservationNotification(Long memberId, Long seatId, Long concertId) {
		NotificationDto notification = NotificationDto.of(
			"티켓 예약이 완료되었습니다.",
			seatId,
			concertId
		);

		notificationService.sendToMember(memberId, notification, "TICKET_RESERVED");
		log.info("티켓 예약 알림 전송 완료 - 회원 ID: {}, 좌석 ID: {}", memberId, seatId);
	}

	public void sendPaymentNotification(Long memberId, String message) {
		NotificationDto notification = NotificationDto.of(message, null, null);
		notificationService.sendToMember(memberId, notification, "PAYMENT_STATUS");
		log.info("결제 상태 알림 전송 완료 - 회원 ID: {}", memberId);
	}

	public void sendGeneralNotification(Long memberId, String message, String eventType) {
		NotificationDto notification = NotificationDto.of(message, null, null);
		notificationService.sendToMember(memberId, notification, eventType);
		log.info("일반 알림 전송 완료 - 회원 ID: {}, 이벤트: {}", memberId, eventType);
	}
}