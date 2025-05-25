package com.flab.tiple.ticket.reservation.application.event.reservation;

import com.flab.tiple.ticket.reservation.application.event.AbstractDomainEvent;

import lombok.Getter;

@Getter
public abstract class TicketNotificationEvent  extends AbstractDomainEvent {
	private final Long memberId;
	private final Long concertId;
	private final String message;

	protected TicketNotificationEvent(Long memberId, Long concertId, String message) {
		super();
		this.memberId = memberId;
		this.concertId = concertId;
		this.message = message;
	}
}
