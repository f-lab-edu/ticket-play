package com.flab.tiple.ticket.reservation.facade;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.flab.tiple.concert.exception.ConcertRemainSeatExistException;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.util.RedissionLockExecutor;
import com.flab.tiple.ticket.reservation.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationInfoResponseDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationResponseDto;
import com.flab.tiple.ticket.reservation.exception.TicketReservationException;
import com.flab.tiple.ticket.reservation.service.TicketReservationService;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingRegisterException;

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

	// 예약 요청 - 락 처리 로직은 그대로 유지하되 결과 처리 변경
	public TicketReservationResponseDto<?> requestReservationFacade(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		// 좌석에 대한 락
		String seatLockName = SEAT_LOCK_PREFIX + requestDto.getSeatId();

		// 결과를 저장할 변수
		final AtomicReference<TicketReservationResponseDto<?>> responseDto = new AtomicReference<>();

		try {
			distributeLockExecutor.execute(seatLockName, LOCK_WAIT_TIME_MS, LOCK_LEASE_TIME_MS, () -> {
				// 대기열 번호에 대한 락
				String concertWaitingLockKey = WAITING_LOCK_PREFIX + requestDto.getConcertId();
				try {
					distributeLockExecutor.execute(concertWaitingLockKey, LOCK_WAIT_TIME_MS, LOCK_LEASE_TIME_MS, () -> {
						// 실제 비즈니스 로직 실행
						responseDto.set(ticketReservationService.requestReservation(requestDto, memberId));
					});
				} catch (IllegalStateException e) {
					log.error("대기열 락 획득 실패: {}", concertWaitingLockKey);
					throw new TicketWaitingRegisterException(
						ErrorCode.TICKET_WAITING_ERROR,
						"대기열 번호 할당 중 오류가 발생했습니다. 다시 시도해주세요."
					);
				}
			});
			return responseDto.get();
		} catch (IllegalStateException e) {
			// 좌석 락 획득 실패 시
			log.error("좌석 락 획득 실패: {}", seatLockName);
			throw new ConcertRemainSeatExistException(
				ErrorCode.CONCERT_SEAT_RESERVATION_NOT_POSSIBLE,
				"현재 다른 사용자가 같은 좌석을 처리 중입니다. 잠시 후 다시 시도해주세요."
			);
		}
	}

	// 티켓 예약 승인 - 결제 완료 후 호출됨
	public TicketReservationInfoResponseDto approveReservationFacade(
		Long reservationId,
		Long memberId
	) {
		// 락 처리 추가
		String reservationLockName = SEAT_LOCK_PREFIX + "approve:" + reservationId;

		final AtomicReference<TicketReservationInfoResponseDto> responseDto = new AtomicReference<>();

		try {
			distributeLockExecutor.execute(reservationLockName, LOCK_WAIT_TIME_MS, LOCK_LEASE_TIME_MS, () -> {
				responseDto.set(ticketReservationService.approveReservation(reservationId, memberId));
			});
			return responseDto.get();
		} catch (IllegalStateException e) {
			log.error("예약 승인 락 획득 실패: {}", reservationLockName);
			throw new TicketReservationException(
				ErrorCode.TICKET_RESERVATION_ERROR,
				"예약 승인 처리 중 오류가 발생했습니다. 다시 시도해주세요."
			);
		}
	}

	// 티켓 예약 취소
	public TicketReservationInfoResponseDto cancelReservationFacade(
		Long reservationId,
		Long memberId
	) {
		String cancelLockName = SEAT_LOCK_PREFIX + "cancel:" + reservationId;

		final AtomicReference<TicketReservationInfoResponseDto> responseDto = new AtomicReference<>();

		try {
			distributeLockExecutor.execute(cancelLockName, LOCK_WAIT_TIME_MS, LOCK_LEASE_TIME_MS, () -> {
				responseDto.set(ticketReservationService.cancelReservation(reservationId, memberId));
			});
			return responseDto.get();
		} catch (IllegalStateException e) {
			log.error("예약 취소 락 획득 실패: {}", cancelLockName);
			throw new TicketReservationException(
				ErrorCode.TICKET_RESERVATION_ERROR,
				"예약 취소 처리 중 오류가 발생했습니다. 다시 시도해주세요."
			);
		}
	}

	// 회원의 티켓 예약 목록 조회
	public List<TicketReservationInfoResponseDto> getMemberReservationsFacade(Long memberId) {
		return ticketReservationService.getMemberReservations(memberId);
	}
}
