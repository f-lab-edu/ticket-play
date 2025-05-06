package com.flab.tiple.ticket.reservation.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flab.tiple.global.auth.aop.LoginCheck;
import com.flab.tiple.global.auth.aop.LoginCheckAspect;
import com.flab.tiple.global.response.ApiResponse;
import com.flab.tiple.ticket.reservation.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationInfoResponseDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationResponseDto;
import com.flab.tiple.ticket.reservation.service.TicketReservationService;

import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/ticket-reservations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ticket-reservation", description = "티켓 예약 관련 API")
public class TicketReservationController {
	private final TicketReservationService ticketReservationService;
	private final MeterRegistry meterRegistry;

	// 티켓 예약 요청
	@PostMapping("/request")
	@LoginCheck(required = true)
	public ApiResponse<TicketReservationResponseDto<?>> requestReservation(
		@RequestBody TicketReservationRequestDto requestDto
	) {
		long startTime = System.currentTimeMillis();
		String traceId = UUID.randomUUID().toString();
		Long memberId = LoginCheckAspect.getCurrentMemberId();
		TicketReservationResponseDto<?> reservation = ticketReservationService.requestReservation(requestDto, memberId, traceId);
		long totalTime = System.currentTimeMillis() - startTime;
		MDC.put("totalTime", String.valueOf(totalTime));

		// CPU 사용량
		double processCpuUsage = meterRegistry.get("process.cpu.usage").gauge().value() * 100;
		double systemCpuUsage = meterRegistry.get("system.cpu.usage").gauge().value() * 100;

		log.info("티켓 예약 요청 완료 시간:{}, traceId:{}, 예약 상태:{}, Memory Usage: {}/{} MB, Process CPU: {}%, System CPU: {}%"
			, totalTime, traceId, reservation.getStatus(), processCpuUsage, systemCpuUsage);
		return ApiResponse.success(reservation);
	}

	// 티켓 예약 승인
	@PostMapping("/{reservationId}/approve")
	@LoginCheck(required = true)
	public ApiResponse<TicketReservationInfoResponseDto> approveReservation(
		@PathVariable Long reservationId
	) {
		Long memberId = LoginCheckAspect.getCurrentMemberId();
		TicketReservationInfoResponseDto reservation = ticketReservationService.approveReservation(reservationId, memberId);
		return ApiResponse.success(reservation);
	}

	// 티켓 예약 취소
	@DeleteMapping("/{reservationId}/cancel")
	@LoginCheck(required = true)
	public ApiResponse<TicketReservationInfoResponseDto> cancelReservation(
		@PathVariable Long reservationId
	) {
		Long memberId = LoginCheckAspect.getCurrentMemberId();
		TicketReservationInfoResponseDto reservation = ticketReservationService.cancelReservation(reservationId, memberId);
		return ApiResponse.success(reservation);
	}

	// 회원의 티켓 예약 목록 조회
	@GetMapping("/my-reservations")
	@LoginCheck(required = true)
	public ApiResponse<List<TicketReservationInfoResponseDto>> getMemberReservations() {
		Long memberId = LoginCheckAspect.getCurrentMemberId();
		List<TicketReservationInfoResponseDto> reservations = ticketReservationService.getMemberReservations(memberId);
		return ApiResponse.success(reservations);
	}
}
