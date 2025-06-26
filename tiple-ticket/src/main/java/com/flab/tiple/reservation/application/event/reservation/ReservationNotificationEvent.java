package com.flab.tiple.reservation.application.event.reservation;


import com.flab.tiple.reservation.domain.model.enums.TicketReservationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ReservationNotificationEvent extends ReservationDomainEvent {
	private final TicketReservationStatus status;
	private final String message;

	@Builder
	public ReservationNotificationEvent(Long reservationId, Long seatId, Long memberId, Long concertId,
		TicketReservationStatus status, String message) {
		super(reservationId, seatId, memberId, concertId);
		this.status = status;
		this.message = message;
	}
}