package com.flab.tiple.ticket.reservation.service;

import java.util.List;

import com.flab.tiple.ticket.reservation.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationInfoResponseDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationResponseDto;

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
}