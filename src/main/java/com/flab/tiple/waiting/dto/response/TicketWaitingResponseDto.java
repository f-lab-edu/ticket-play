package com.flab.tiple.waiting.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketWaitingResponseDto {
	private Integer waitingNumber;
	private Long concertId;
	private String concertName;
	private String message;
	private String status;

	@Builder
	public TicketWaitingResponseDto(Integer waitingNumber, Long concertId, String concertName, String status) {
		this.waitingNumber = waitingNumber;
		this.concertId = concertId;
		this.concertName = concertName;
		this.message = "웨이팅 리스트에 등록되었습니다. 현재 대기순번은 " +
			waitingNumber + "번입니다.";;
		this.status = status;
	}
}
