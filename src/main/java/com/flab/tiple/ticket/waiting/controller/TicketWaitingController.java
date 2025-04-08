package com.flab.tiple.ticket.waiting.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flab.tiple.global.auth.aop.LoginCheck;
import com.flab.tiple.global.auth.aop.LoginCheckAspect;
import com.flab.tiple.global.response.ApiResponse;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingInfoResponseDto;
import com.flab.tiple.ticket.waiting.service.TicketWaitingService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ticket-waiting")
@RequiredArgsConstructor
@Tag(name = "ticket-waiting", description = "티켓 웨이팅 관련 API")
public class TicketWaitingController {
	private final TicketWaitingService ticketWaitingService;

	@DeleteMapping("/{waitingId}/cancel")
	@LoginCheck
	public ApiResponse<TicketWaitingCancelResponseDto> cancelWaiting(
		@PathVariable Long waitingId
	) {
		Long memberId = LoginCheckAspect.getCurrentMemberId();
		TicketWaitingCancelResponseDto waiting = ticketWaitingService.cancelWaiting(waitingId, memberId);
		return ApiResponse.success(waiting);
	}

	@GetMapping("/my-waiting")
	@LoginCheck(required = true)
	public ApiResponse<List<TicketWaitingInfoResponseDto>> getMemberWaitingList() {
		Long memberId = LoginCheckAspect.getCurrentMemberId();
		List<TicketWaitingInfoResponseDto> waitingList = ticketWaitingService.getMemberWaitingList(memberId);
		return ApiResponse.success(waitingList);
	}
}
