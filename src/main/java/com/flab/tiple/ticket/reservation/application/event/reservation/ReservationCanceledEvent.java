package com.flab.tiple.ticket.reservation.application.event.reservation;

import com.flab.tiple.ticket.reservation.domain.model.TicketReservation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ReservationCanceledEvent extends ReservationDomainEvent {

	@Builder
	public ReservationCanceledEvent(Long reservationId, Long seatId, Long memberId, Long concertId) {
		super(reservationId, seatId, memberId, concertId);
	}

	public static ReservationCanceledEvent from(TicketReservation reservation) {
		return ReservationCanceledEvent.builder()
			.reservationId(reservation.getId())
			.seatId(reservation.getSeat().getId())
			.memberId(reservation.getMember().getId())
			.concertId(reservation.getSeat().getConcert().getId())
			.build();
	}
}
