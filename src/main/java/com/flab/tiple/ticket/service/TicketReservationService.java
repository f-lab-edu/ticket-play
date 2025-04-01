package com.flab.tiple.ticket.service;

import java.util.List;

import com.flab.tiple.ticket.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.dto.response.TicketReservationResponseDto;

public interface TicketReservationService {
	TicketReservationResponseDto requestReservation(
		TicketReservationRequestDto requestDto,
		Long memberId
	);
	TicketReservationResponseDto approveReservation(Long reservationId, Long memberId);
	TicketReservationResponseDto cancelReservation(
		Long reservationId,
		Long memberId
	);
	List<TicketReservationResponseDto> getMemberReservations(Long memberId);
}