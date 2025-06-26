package com.flab.tiple.reservation.application.event.reservation;


import com.flab.tiple.reservation.domain.model.TicketReservation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ReservationApprovedEvent extends ReservationDomainEvent {
	@Builder
	public ReservationApprovedEvent(Long reservationId, Long seatId, Long memberId, Long concertId) {
		super(reservationId, seatId, memberId, concertId);
	}

	public static ReservationApprovedEvent from(TicketReservation reservation) {
		return ReservationApprovedEvent.builder()
			.reservationId(reservation.getId())
			.seatId(reservation.getSeat().getId())
			.memberId(reservation.getMember().getId())
			.concertId(reservation.getSeat().getConcert().getId())
			.build();
	}
}