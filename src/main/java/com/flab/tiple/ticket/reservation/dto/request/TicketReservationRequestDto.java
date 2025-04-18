package com.flab.tiple.ticket.reservation.dto.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketReservationRequestDto {
	private Long seatId;
	private Long concertId;

	@Builder
	public TicketReservationRequestDto(Long seatId) {
		this.seatId = seatId;
	}
}
