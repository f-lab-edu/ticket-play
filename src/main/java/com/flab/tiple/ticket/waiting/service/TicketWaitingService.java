package com.flab.tiple.ticket.waiting.service;

import java.util.List;

import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingInfoResponseDto;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingResponseDto;

public interface TicketWaitingService {
	TicketWaitingCancelResponseDto cancelWaiting(
		Long waitingId,
		Long memberId
	);

	List<TicketWaitingInfoResponseDto> getMemberWaitingList(Long memberId);
}
