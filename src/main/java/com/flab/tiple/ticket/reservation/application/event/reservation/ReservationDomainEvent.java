package com.flab.tiple.ticket.reservation.application.event.reservation;

import com.flab.tiple.ticket.reservation.application.event.AbstractDomainEvent;

import lombok.Getter;

@Getter
public abstract class ReservationDomainEvent extends AbstractDomainEvent {
	private final Long reservationId;
	private final Long seatId;
	private final Long memberId;
	private final Long concertId;

	protected ReservationDomainEvent(Long reservationId, Long seatId, Long memberId, Long concertId) {
		super();
		this.reservationId = reservationId;
		this.seatId = seatId;
		this.memberId = memberId;
		this.concertId = concertId;
	}

}