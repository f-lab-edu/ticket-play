package com.flab.tiple.ticket.reservation.facade;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.flab.tiple.concert.exception.ConcertRemainSeatExistException;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.util.RedissionLockExecutor;
import com.flab.tiple.ticket.reservation.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationInfoResponseDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationResponseDto;
import com.flab.tiple.ticket.reservation.service.TicketReservationService;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingRegisterException;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketReservationFacade {
	private final TicketReservationService ticketReservationService;
	private final RedissionLockExecutor distributeLockExecutor;

	public TicketReservationResponseDto<?> requestReservationFacade(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		// 좌석에 대한 락
		String seatLockName = "lockTicketReservation:" + requestDto.getSeatId();
		final long waitTime = 10000; // 10초
		final long leaseTime = 30000; // 30초

		// 결과를 저장할 변수
		final AtomicReference<TicketReservationResponseDto<?>> responseDto = new AtomicReference<>();

		try {
			distributeLockExecutor.execute(seatLockName, waitTime, leaseTime, () -> {
				// 대기열 번호에 대한 락
				String concertWaitingLockKey = "lock:waiting:" + requestDto.getConcertId();
				try {
					distributeLockExecutor.execute(concertWaitingLockKey, waitTime, leaseTime, () -> {
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

	// 티켓 예약 승인
	public TicketReservationInfoResponseDto approveReservationFacade(
		Long reservationId,
		Long memberId
	) {
		return ticketReservationService.approveReservation(reservationId, memberId);
	}

	// 티켓 예약 취소
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