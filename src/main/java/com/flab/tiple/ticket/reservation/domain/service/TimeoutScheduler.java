package com.flab.tiple.ticket.reservation.domain.service;

import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationCreatedEvent;

public interface TimeoutScheduler {
	void scheduleTimeout(ReservationCreatedEvent event);
	void cancelTimeout(Long reservationId, Long seatId, Long memberId, Long concertId);
}