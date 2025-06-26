package com.flab.tiple.presentation.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flab.tiple.application.SseNotificationUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationEventController {
	private final SseNotificationUseCase sseNotificationUseCase;

	@PostMapping("/ticket-reserved")
	public void handleTicketReserved(
		@RequestParam Long memberId,
		@RequestParam Long seatId,
		@RequestParam Long concertId) {
		sseNotificationUseCase.sendTicketReservationNotification(memberId, seatId, concertId);
	}

	@PostMapping("/payment-status")
	public void handlePaymentStatus(
		@RequestParam Long memberId,
		@RequestParam String message) {
		sseNotificationUseCase.sendPaymentNotification(memberId, message);
	}
}