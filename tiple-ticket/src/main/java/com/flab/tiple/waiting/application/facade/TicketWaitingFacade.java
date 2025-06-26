package com.flab.tiple.waiting.application.facade;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.flab.tiple.exception.ErrorCode;
import com.flab.tiple.util.RedissionLockExecutor;
import com.flab.tiple.waiting.application.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.waiting.application.dto.response.TicketWaitingInfoResponseDto;
import com.flab.tiple.waiting.domain.exception.TicketWaitingException;
import com.flab.tiple.waiting.domain.service.TicketWaitingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketWaitingFacade {
	private final TicketWaitingService ticketWaitingService;
	private final RedissionLockExecutor distributeLockExecutor;

	// 클래스 레벨 상수로 정의
	private final long LOCK_WAIT_TIME_MS = 10000; // 10초
	private final long LOCK_LEASE_TIME_MS = 30000; // 30초
	private final String WAITING_LOCK_PREFIX = "lock:waiting:";

	// 대기 취소 - 분산 락 적용
	public TicketWaitingCancelResponseDto cancelWaitingFacade(Long waitingId, Long memberId) {
		String cancelLockName = WAITING_LOCK_PREFIX + "cancel:" + waitingId;

		final AtomicReference<TicketWaitingCancelResponseDto> responseDto = new AtomicReference<>();

		try {
			distributeLockExecutor.execute(cancelLockName, LOCK_WAIT_TIME_MS, LOCK_LEASE_TIME_MS, () -> {
				responseDto.set(ticketWaitingService.cancelWaiting(waitingId, memberId));
			});
			return responseDto.get();
		} catch (IllegalStateException e) {
			log.error("대기 취소 락 획득 실패: {}", cancelLockName);
			throw new TicketWaitingException(
				ErrorCode.TICKET_WAITING_ERROR,
				ErrorCode.TICKET_WAITING_ERROR.getDescription()
			);
		}
	}

	// 대기 목록 조회 - 락 필요 없음
	public List<TicketWaitingInfoResponseDto> getMemberWaitingListFacade(Long memberId) {
		return ticketWaitingService.getMemberWaitingList(memberId);
	}

}