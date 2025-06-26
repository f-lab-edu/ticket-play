package com.flab.tiple.waiting.presentaion;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flab.tiple.aop.login.LoginCheck;
import com.flab.tiple.aop.login.LoginCheckAspect;
import com.flab.tiple.response.ApiResponse;
import com.flab.tiple.waiting.application.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.waiting.application.dto.response.TicketWaitingInfoResponseDto;
import com.flab.tiple.waiting.application.facade.TicketWaitingFacade;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ticket-waiting")
@RequiredArgsConstructor
@Tag(name = "ticket-waiting", description = "티켓 대기 관련 API")
public class TicketWaitingController {
	private final TicketWaitingFacade ticketWaitingFacade;

	// 티켓 대기 취소
	@DeleteMapping("/{waitingId}/cancel")
	@LoginCheck(required = true)
	public ApiResponse<TicketWaitingCancelResponseDto> cancelWaiting(
		@PathVariable Long waitingId
	) {
		Long memberId = LoginCheckAspect.getCurrentMemberId();
		TicketWaitingCancelResponseDto result = ticketWaitingFacade.cancelWaitingFacade(waitingId, memberId);
		return ApiResponse.success(result);
	}

	// 회원의 대기 목록 조회
	@GetMapping("/my-waiting")
	@LoginCheck(required = true)
	public ApiResponse<List<TicketWaitingInfoResponseDto>> getMemberWaitingList() {
		Long memberId = LoginCheckAspect.getCurrentMemberId();
		List<TicketWaitingInfoResponseDto> waitingList = ticketWaitingFacade.getMemberWaitingListFacade(memberId);
		return ApiResponse.success(waitingList);
	}

}
