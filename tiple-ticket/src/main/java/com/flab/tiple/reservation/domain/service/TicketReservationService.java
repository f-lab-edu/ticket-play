package com.flab.tiple.reservation.domain.service;

import java.util.List;

import com.flab.tiple.reservation.application.dto.request.TicketReservationRequestDto;
import com.flab.tiple.reservation.application.dto.response.TicketReservationInfoResponseDto;
import com.flab.tiple.reservation.application.dto.response.TicketReservationResponseDto;

public interface TicketReservationService {
	TicketReservationResponseDto requestReservation(
		TicketReservationRequestDto requestDto,
		Long memberId
	);
	TicketReservationInfoResponseDto approveReservation(Long reservationId, Long memberId);
	TicketReservationInfoResponseDto cancelReservation(
		Long reservationId,
		Long memberId
	);
	List<TicketReservationInfoResponseDto> getMemberReservations(Long memberId);
	void handleReservationTimeout(Long seatId, Long memberId);
}