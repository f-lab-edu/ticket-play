package com.flab.tiple.ticket.waiting.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketWaitingInfoResponseDto {
	private Integer waitingNumber;
	private Long concertId;
	private String status;

	@Builder
	public TicketWaitingInfoResponseDto(Integer waitingNumber, Long concertId, String status) {
		this.waitingNumber = waitingNumber;
		this.concertId = concertId;
		this.status = status;
	}
}

