package com.flab.tiple.ticket.reservation.facade;

import org.springframework.stereotype.Component;

import com.flab.tiple.concert.domain.ConcertSeat;
import com.flab.tiple.concert.exception.ConcertSeatNotFoundException;
import com.flab.tiple.concert.repository.concertSeat.ConcertSeatRepository;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.service.NamedLockRepository;
import com.flab.tiple.ticket.reservation.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationResponseDto;
import com.flab.tiple.ticket.reservation.enums.TicketProcessStatus;
import com.flab.tiple.ticket.reservation.service.TicketReservationService;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingResponseDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketReservationFacade {
	private final TicketReservationService ticketReservationService;
	private final NamedLockRepository namedLockRepository;

	public TicketReservationResponseDto<?> requestReservationFacade(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		// 기존 좌석 락 로직 유지
		String seatLockKey = "lock:" + String.valueOf(requestDto.getSeatId());
		TicketReservationResponseDto<?> ticketReservationResponseDto;

		try {
			namedLockRepository.getLock(seatLockKey);
			// 콘서트 대기열에 대한 락 추가
			String concertWaitingLockKey = "lock:waiting:" + requestDto.getConcertId();
			try {
				namedLockRepository.getLock(concertWaitingLockKey);
				ticketReservationResponseDto = ticketReservationService.requestReservation(requestDto, memberId);
			} finally {
				namedLockRepository.releaseLock(concertWaitingLockKey);
			}
		} finally {
			namedLockRepository.releaseLock(seatLockKey);
		}

		return ticketReservationResponseDto;
	}
}