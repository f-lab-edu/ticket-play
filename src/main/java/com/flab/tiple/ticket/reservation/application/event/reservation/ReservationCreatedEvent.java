package com.flab.tiple.ticket.reservation.application.event.reservation;

import com.flab.tiple.ticket.reservation.application.event.AbstractDomainEvent;
import com.flab.tiple.ticket.reservation.domain.model.TicketReservation;
import com.flab.tiple.ticket.reservation.domain.model.enums.TicketReservationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ReservationCreatedEvent extends ReservationDomainEvent {
	private final TicketReservationStatus status;

	@Builder
	public ReservationCreatedEvent(Long reservationId, Long seatId, Long memberId, Long concertId, TicketReservationStatus status) {
		super(reservationId, seatId, memberId, concertId);
		this.status = status;
	}

	public static ReservationCreatedEvent from(TicketReservation reservation) {
		return ReservationCreatedEvent.builder()
			.reservationId(reservation.getId())
			.seatId(reservation.getSeat().getId())
			.memberId(reservation.getMember().getId())
			.concertId(reservation.getSeat().getConcert().getId())
			.status(reservation.getStatus())
			.build();
	}
}