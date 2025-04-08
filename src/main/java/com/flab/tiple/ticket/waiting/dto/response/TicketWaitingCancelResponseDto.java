package com.flab.tiple.ticket.waiting.dto.response;

import com.flab.tiple.ticket.waiting.enums.TicketWaitingStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketWaitingCancelResponseDto {
	private Long id;
	private TicketWaitingStatus status;
	private String deletedAt;

	@Builder
	public TicketWaitingCancelResponseDto(Long id,TicketWaitingStatus status, String deletedAt) {
		this.id = id;
		this.status = status;
		this.deletedAt = deletedAt;
	}

}
