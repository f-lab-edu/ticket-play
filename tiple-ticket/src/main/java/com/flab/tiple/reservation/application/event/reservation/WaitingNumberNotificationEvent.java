package com.flab.tiple.reservation.application.event.reservation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class WaitingNumberNotificationEvent extends TicketNotificationEvent {
	private final Integer waitingNumber;

	@Builder
	public WaitingNumberNotificationEvent(Long memberId, Long concertId, String message, Integer waitingNumber) {
		super(memberId, concertId, message);
		this.waitingNumber = waitingNumber;
	}
}