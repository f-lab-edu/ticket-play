package com.flab.tiple.ticket.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketReservationTimeoutData {
	private Long reservationId;
	private Long seatId;
	private Long memberId;
	private Long concertId;
}