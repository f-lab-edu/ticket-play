package com.flab.tiple.concert.service;

import org.springframework.data.domain.Pageable;

import com.flab.tiple.concert.dto.response.ConcertDetailResponseDto;
import com.flab.tiple.concert.dto.response.ConcertResponseDto;
import com.flab.tiple.concert.dto.response.ConcertSeatResponseDto;
import com.flab.tiple.global.response.PageResponse;

public interface ConcertService {
	PageResponse<ConcertResponseDto> getAllConcerts(Pageable pageable); // 모든 콘서트 조회
	PageResponse<ConcertResponseDto> getAvailableReservationConcerts(Pageable pageable); // 예매 가능한 콘서트 조회
	ConcertDetailResponseDto getConcertById(Long id); // 콘서트 정보 상세조회
	PageResponse<ConcertSeatResponseDto> getConcertSeats(Long id,Pageable pageable); //콘서트 좌석정보 조회
}
