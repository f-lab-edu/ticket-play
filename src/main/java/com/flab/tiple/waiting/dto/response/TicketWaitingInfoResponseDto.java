package com.flab.tiple.waiting.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketWaitingInfoResponseDto {
	private Integer waitingNumber;
	private Long concertId;
	private String concertName;
	private String status;

	@Builder
	public TicketWaitingInfoResponseDto(Integer waitingNumber, Long concertId, String concertName, String status) {
		this.waitingNumber = waitingNumber;
		this.concertId = concertId;
		this.concertName = concertName;
		this.status = status;
	}
}
