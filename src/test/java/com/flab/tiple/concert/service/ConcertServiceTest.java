package com.flab.tiple.concert.service;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.concert.dto.response.ConcertDetailResponseDto;
import com.flab.tiple.concert.dto.response.ConcertResponseDto;
import com.flab.tiple.concert.enums.ConcertStatus;
import com.flab.tiple.concert.repository.concert.ConcertRepository;
import com.flab.tiple.concert.repository.concertSeat.ConcertSeatRepository;
import com.flab.tiple.global.response.PageResponse;

@ExtendWith(MockitoExtension.class)
public class ConcertServiceTest {
	@Mock
	private ConcertRepository concertRepository;

	@Mock
	private ConcertSeatRepository concertSeatRepository;

	@InjectMocks
	private ConcertServiceImpl concertService;

	private Concert createMockConcert(Long id) {
		Concert concert = Concert.builder()
			.name("Shining Star 콘서트")
			.artistName("엑소")
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusHours(2))
			.reservationStartTime(LocalDateTime.now().minusDays(1))
			.reservationEndTime(LocalDateTime.now().plusDays(1))
			.remainingSeat(100)
			.status(ConcertStatus.OPEN)
			.concertHallName("서울 콘서트홀")
			.concertHallAddress("서울시 강남구")
			.concertSeatInfo("일반석, VIP석")
			.build();

		// Reflection을 사용해 ID 설정
		ReflectionTestUtils.setField(concert, "id", id);
		ReflectionTestUtils.setField(concert, "deletedAt", null);
		ReflectionTestUtils.setField(concert, "createdAt", LocalDateTime.now());

		return concert;
	}

	@Test
	@DisplayName("모든 콘서트 목록을 페이징하여 조회한다")
	void getAllConcerts() {
		// given
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

		// 테스트용 콘서트 목록 생성
		List<Concert> concerts = IntStream.range(0, 10)
			.mapToObj(i -> createMockConcert((long) i))
			.collect(Collectors.toList());

		// PageImpl 생성 (deletedAt이 null인 콘서트들)
		Page<Concert> concertPage = new PageImpl<>(concerts, pageable, 19);

		// Mock 메서드 동작 정의 - findAllConcerts 메서드 모의
		when(concertRepository.findAllConcerts(any(Pageable.class))).thenReturn(concertPage);

		// when
		PageResponse<ConcertResponseDto> result = concertService.getAllConcerts(pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(19);
		assertThat(result.getContent().size()).isEqualTo(10);
		assertThat(result.getContent().get(0).getName()).isNotEmpty();
	}

	@Test
	@DisplayName("예매 가능한 콘서트 목록을 페이징하여 조회한다")
	void getAvailableConcerts() {
		// given
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

		// 테스트용 OPEN 상태 콘서트 목록 생성
		List<Concert> concerts = IntStream.range(0, 10)
			.mapToObj(i -> createMockConcert((long) i))
			.collect(Collectors.toList());

		// PageImpl 생성
		Page<Concert> concertPage = new PageImpl<>(concerts, pageable, 19);

		// Mock 메서드 동작 정의
		when(concertRepository.findAvailableConcerts(any(Pageable.class))).thenReturn(concertPage);

		// when
		PageResponse<ConcertResponseDto> result = concertService.getAvailableReservationConcerts(pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).allMatch(concert -> ConcertStatus.OPEN.name().equals(concert.getStatus()));
	}

	@Test
	@DisplayName("특정 콘서트 상세 정보를 조회한다")
	void getConcertById_Success() {
		// given
		Long concertId = 1L;

		// 테스트용 콘서트 생성 (deletedAt을 null로 설정)
		Concert concert = createMockConcert(concertId);

		// Mock 메서드 동작 정의 - deletedAt이 null인 Concert 반환
		when(concertRepository.findByIdAndDeletedAtIsNull(concertId))
			.thenReturn(Optional.of(concert));

		// when
		ConcertDetailResponseDto result = concertService.getConcertById(concertId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(concertId);
		assertThat(result.getName()).isEqualTo("Shining Star 콘서트");
		assertThat(result.getArtistName()).isEqualTo("엑소");
	}
}