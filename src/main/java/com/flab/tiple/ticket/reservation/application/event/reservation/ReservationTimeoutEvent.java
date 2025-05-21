package com.flab.tiple.ticket.reservation.application.event.reservation;

import com.flab.tiple.ticket.reservation.application.event.AbstractDomainEvent;
import com.flab.tiple.ticket.reservation.domain.model.TicketReservation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ReservationTimeoutEvent extends AbstractDomainEvent {
	private final Long reservationId;
	private final Long seatId;
	private final Long memberId;
	private final Long concertId;

	@Builder
	public ReservationTimeoutEvent(Long reservationId, Long seatId, Long memberId, Long concertId) {
		super();
		this.reservationId = reservationId;
		this.seatId = seatId;
		this.memberId = memberId;
		this.concertId = concertId;
	}

	public static ReservationTimeoutEvent from(TicketReservation reservation) {
		return ReservationTimeoutEvent.builder()
			.reservationId(reservation.getId())
			.seatId(reservation.getSeat().getId())
			.memberId(reservation.getMember().getId())
			.concertId(reservation.getSeat().getConcert().getId())
			.build();
	}
}

