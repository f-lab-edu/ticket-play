package com.flab.tiple.concert.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.concert.domain.ConcertSeat;
import com.flab.tiple.concert.dto.response.ConcertDetailResponseDto;
import com.flab.tiple.concert.dto.response.ConcertResponseDto;
import com.flab.tiple.concert.dto.response.ConcertSeatResponseDto;
import com.flab.tiple.concert.exception.ConcertNotFoundException;
import com.flab.tiple.concert.repository.concert.ConcertRepository;
import com.flab.tiple.concert.repository.concertSeat.ConcertSeatRepository;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertServiceImpl implements ConcertService {

	private final ConcertRepository concertRepository;
	private final ConcertSeatRepository concertSeatRepository;

	@Override
	public PageResponse<ConcertResponseDto> getAllConcerts(Pageable pageable) {
		Page<Concert> concertPage = concertRepository.findAllConcerts(pageable);

		return makeConcertPageResponse(concertPage);
	}

	@Override
	public PageResponse<ConcertResponseDto> getAvailableReservationConcerts(Pageable pageable) {
		Page<Concert> availabeReservationConcertPage = concertRepository.findAvailableConcerts(pageable);

		return makeConcertPageResponse(availabeReservationConcertPage);
	}

	@Override
	public ConcertDetailResponseDto getConcertById(Long id) {
		Concert concert = concertRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ConcertNotFoundException(ErrorCode.CONCERT_NOT_FOUND, ErrorCode.CONCERT_NOT_FOUND.getDescription()));
		return mapToConcertDetailResponseDto(concert);
	}

	@Override
	public PageResponse<ConcertSeatResponseDto> getConcertSeats(Long id, Pageable pageable) {
		// 콘서트 존재 여부 확인
		if (concertRepository.findByIdAndDeletedAtIsNull(id).isEmpty()) {
			throw new ConcertNotFoundException(ErrorCode.CONCERT_NOT_FOUND, ErrorCode.CONCERT_NOT_FOUND.getDescription());
		}
		Page<ConcertSeat> concertSeats =  concertSeatRepository.findConcertSeats(id, pageable);

		return makeConcertSeatPageResponse(concertSeats);
	}


	private ConcertSeatResponseDto mapToConcertSeatResponseDto(ConcertSeat seat) {
		return ConcertSeatResponseDto.builder()
			.id(seat.getId())
			.concertId(seat.getConcert().getId())
			.seatNumber(seat.getSeatNumber())
			.grade(seat.getGrade().toString())
			.build();
	}

	private ConcertDetailResponseDto mapToConcertDetailResponseDto(Concert concert) {
		return ConcertDetailResponseDto.builder()
			.id(concert.getId())
			.name(concert.getName())
			.artistName(concert.getArtistName())
			.name(concert.getName())
			.concertHallName(concert.getConcertHallName())
			.concertHallAddress(concert.getConcertHallAddress())
			.remainingSeat(concert.getRemainingSeat())
			.status(concert.getStatus().toString())
			.startTime(concert.getStartTime().toString())
			.endTime(concert.getEndTime().toString())
			.reservationStartTime(concert.getReservationStartTime().toString())
			.reservationEndTime(concert.getReservationEndTime().toString())
			.build();
	}
	private ConcertResponseDto mapToConcertResponseDto(Concert concert) {
		return ConcertResponseDto.builder()
			.id(concert.getId())
			.name(concert.getName())
			.artistName(concert.getArtistName())
			.status(concert.getStatus().toString())
			.name(concert.getName())
			.startTime(concert.getStartTime().toString())
			.endTime(concert.getEndTime().toString())
			.reservationStartTime(concert.getStartTime().toString())
			.reservationEndTime(concert.getReservationEndTime().toString())
			.build();
	}

	private PageResponse<ConcertResponseDto> makeConcertPageResponse(Page<Concert> concertPage) {
		return new PageResponse<>(
			concertPage.getContent().stream().map(this::mapToConcertResponseDto).toList(),
			concertPage.getNumber(),
			concertPage.getTotalPages(),
			concertPage.getTotalElements(),
			concertPage.hasNext()
		);
	}

	private PageResponse<ConcertSeatResponseDto> makeConcertSeatPageResponse(Page<ConcertSeat> concertSeatPage) {
		return new PageResponse<>(
			concertSeatPage.getContent().stream().map(this::mapToConcertSeatResponseDto).toList(),
			concertSeatPage.getNumber(),
			concertSeatPage.getTotalPages(),
			concertSeatPage.getTotalElements(),
			concertSeatPage.hasNext()
		);
	}
}
