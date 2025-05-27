package com.flab.tiple.ticket.reservation.application.event.reservation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class SeatAvailableNotificationEvent extends ReservationDomainEvent {
	private final String message;

	@Builder
	public SeatAvailableNotificationEvent(Long reservationId, Long seatId, Long memberId, Long concertId, String message) {
		super(reservationId, seatId, memberId, concertId);
		this.message = message;
	}
}