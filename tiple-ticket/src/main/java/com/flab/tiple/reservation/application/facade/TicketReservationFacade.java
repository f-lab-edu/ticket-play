package com.flab.tiple.reservation.application.facade;

import java.util.List;

import org.springframework.stereotype.Component;

import com.flab.tiple.aop.lock.DistributedLockRetry;
import com.flab.tiple.aop.lock.LockIdType;
import com.flab.tiple.reservation.application.dto.request.TicketReservationRequestDto;
import com.flab.tiple.reservation.application.dto.response.TicketReservationInfoResponseDto;
import com.flab.tiple.reservation.application.dto.response.TicketReservationResponseDto;
import com.flab.tiple.reservation.domain.service.TicketReservationService;
import com.flab.tiple.util.RedissionLockExecutor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketReservationFacade {
	private final TicketReservationService ticketReservationService;
	private final RedissionLockExecutor distributeLockExecutor;

	// 클래스 레벨 상수로 정의
	private final long LOCK_WAIT_TIME_MS = 10000; // 10초
	private final long LOCK_LEASE_TIME_MS = 30000; // 30초
	private final String SEAT_LOCK_PREFIX = "lockTicketReservation:";
	private final String WAITING_LOCK_PREFIX = "lock:waiting:";
	private final String APPROVE_LOCK_PREFIX = "lock:approve:";
	private final String CANCEL_LOCK_PREFIX = "lock:cancel:";

	// 예약 요청 - 락 처리 로직은 그대로 유지하되 결과 처리 변경
	// 예시 2: 복합 락 + PARAMETER 타입
	@DistributedLockRetry(
		lockPrefix = SEAT_LOCK_PREFIX,
		secondaryLockPrefix = WAITING_LOCK_PREFIX,
		compoundLock = true,                     // 복합 락 사용
		waitTimeMs = LOCK_WAIT_TIME_MS,
		leaseTimeMs = LOCK_LEASE_TIME_MS,
		idParameterIndex = 0,                    // DTO에서 seatId 추출
		lockIdType = LockIdType.PARAMETER
	)
	public TicketReservationResponseDto<?> requestReservationFacade(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		return ticketReservationService.requestReservation(requestDto, memberId);
	}


	// 티켓 예약 승인 - 결제 완료 후 호출됨
	@DistributedLockRetry(
		lockPrefix = APPROVE_LOCK_PREFIX,
		waitTimeMs = LOCK_WAIT_TIME_MS,
		leaseTimeMs = LOCK_LEASE_TIME_MS,
		idParameterIndex = 0,           // 첫 번째 파라미터(reservationId) 사용
		lockIdType = LockIdType.PARAMETER
	)
	public TicketReservationInfoResponseDto approveReservationFacade(
		Long reservationId,
		Long memberId
	) {
		return ticketReservationService.approveReservation(reservationId, memberId);
	}


	// 티켓 예약 취소
	@DistributedLockRetry(
		lockPrefix = CANCEL_LOCK_PREFIX,
		waitTimeMs = LOCK_WAIT_TIME_MS,
		leaseTimeMs = LOCK_LEASE_TIME_MS,
		idParameterIndex = 0,  // 첫 번째 파라미터(reservationId)를 락 ID로 사용
		lockIdType = LockIdType.PARAMETER
	)
	public TicketReservationInfoResponseDto cancelReservationFacade(
		Long reservationId,
		Long memberId
	) {
		return ticketReservationService.cancelReservation(reservationId, memberId);
	}

	// 회원의 티켓 예약 목록 조회
	public List<TicketReservationInfoResponseDto> getMemberReservationsFacade(Long memberId) {
		return ticketReservationService.getMemberReservations(memberId);
	}
}
