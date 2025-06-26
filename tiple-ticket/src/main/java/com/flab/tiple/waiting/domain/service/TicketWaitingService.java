package com.flab.tiple.waiting.domain.service;

import java.util.List;

import com.flab.tiple.waiting.application.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.waiting.application.dto.response.TicketWaitingInfoResponseDto;

public interface TicketWaitingService {
	TicketWaitingCancelResponseDto cancelWaiting(
		Long waitingId,
		Long memberId
	);

	List<TicketWaitingInfoResponseDto> getMemberWaitingList(Long memberId);
}
