package com.flab.tiple.concert.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flab.tiple.concert.dto.response.ConcertDetailResponseDto;
import com.flab.tiple.concert.dto.response.ConcertResponseDto;
import com.flab.tiple.concert.dto.response.ConcertSeatResponseDto;
import com.flab.tiple.concert.service.ConcertService;
import com.flab.tiple.global.response.ApiResponse;
import com.flab.tiple.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {
	private final ConcertService concertService;

	/**
	 * 모든 콘서트 목록 조회 (페이징 처리)
     */
	@GetMapping
	public ApiResponse<PageResponse<ConcertResponseDto>> getAllConcerts(
		@PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

		PageResponse<ConcertResponseDto> response = concertService.getAllConcerts(pageable);
		return ApiResponse.success(response);
	}

	/**
	 * 예매 가능한 콘서트 목록 조회 (페이징 처리)
	 */
	@GetMapping("/available")
	public ApiResponse<PageResponse<ConcertResponseDto>> getAvailableConcerts(
		@PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

		PageResponse<ConcertResponseDto> response = concertService.getAvailableReservationConcerts(pageable);

		return ApiResponse.success(response);
	}

	/**
	 * 콘서트 상세 정보 조회
	 */
	@GetMapping("/{concertId}")
	public ApiResponse<ConcertDetailResponseDto> getConcertDetail(@PathVariable Long concertId) {
		ConcertDetailResponseDto response = concertService.getConcertById(concertId);
		return ApiResponse.success(response);
	}

	/**
	 * 콘서트 좌석 정보 조회 (페이징 처리)
	 */
	@GetMapping("/{concertId}/seats")
	public ApiResponse<PageResponse<ConcertSeatResponseDto>> getConcertSeats(
		@PathVariable Long concertId,
		@PageableDefault(page = 0, size = 50, sort = "seatNumber", direction = Sort.Direction.ASC) Pageable pageable) {

		PageResponse<ConcertSeatResponseDto> response = concertService.getConcertSeats(concertId, pageable);
		return ApiResponse.success(response);
	}
}
