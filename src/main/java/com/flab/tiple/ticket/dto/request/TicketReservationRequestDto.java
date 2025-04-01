package com.flab.tiple.ticket.dto.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketReservationRequestDto {
	private Long seatId;

	@Builder
	public TicketReservationRequestDto(Long seatId) {
		this.seatId = seatId;
	}
}
