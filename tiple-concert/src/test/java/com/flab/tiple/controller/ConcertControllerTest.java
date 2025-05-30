package com.flab.tiple.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.application.dto.response.ConcertDetailResponseDto;
import com.flab.tiple.application.dto.response.ConcertResponseDto;
import com.flab.tiple.application.dto.response.ConcertSeatResponseDto;
import com.flab.tiple.application.service.ConcertService;
import com.flab.tiple.config.AbstractRestDocs;
import com.flab.tiple.domain.enums.ConcertStatus;
import com.flab.tiple.presentation.api.ConcertController;
import com.flab.tiple.response.ApiResponse;
import com.flab.tiple.response.PageResponse;
import com.flab.tiple.security.CustomUserDetailsService;
import com.flab.tiple.security.JwtTokenProvider;
import com.flab.tiple.security.SecurityConfig;
import com.flab.tiple.util.JwtTokenUtil;

@WebMvcTest({ConcertController.class, SecurityConfig.class})
public class ConcertControllerTest  extends AbstractRestDocs {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ConcertService concertService;

	// SecurityConfig에 필요한 의존성 MockBean으로 추가
	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private ObjectMapper objectMapper;

	private List<ConcertResponseDto> createMockConcertResponseDtos(int count) {
		return IntStream.range(0, count)
			.mapToObj(i -> ConcertResponseDto.builder()
				.id((long) i)
				.name("콘서트 " + i)
				.artistName("아티스트 " + i)
				.status(ConcertStatus.OPEN.name())
				.build())
			.collect(Collectors.toList());
	}

	@Test
	@DisplayName("모든 콘서트 목록을 페이징하여 조회한다")
	void getAllConcerts() throws Exception {
		// given
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
		List<ConcertResponseDto> mockConcerts = createMockConcertResponseDtos(10);

		PageResponse<ConcertResponseDto> responseDto = new PageResponse<>(
			mockConcerts,
			0,
			2,
			19,
			true
		);

		given(concertService.getAllConcerts(any(Pageable.class)))
			.willReturn(responseDto);

		// when
		ResultActions resultActions = mockMvc.perform(
			get("/api/concerts")
				.param("page", "0")
				.param("size", "10")
				.param("sort", "createdAt,desc"));

		// then
		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
			.andReturn().getResponse();

		ApiResponse<PageResponse<ConcertResponseDto>> apiResponse = objectMapper.readValue(
			response.getContentAsString(),
			new TypeReference<ApiResponse<PageResponse<ConcertResponseDto>>>() {}
		);

		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");

		// PageResponse 객체의 내용 검증
		PageResponse<ConcertResponseDto> pageData = apiResponse.getData();
		Assertions.assertThat(pageData.getContent()).hasSize(10);
		Assertions.assertThat(pageData.getTotalElements()).isEqualTo(19);
		Assertions.assertThat(pageData.getCurrentPage()).isEqualTo(0);
		Assertions.assertThat(pageData.getTotalPages()).isEqualTo(2);
		Assertions.assertThat(pageData.isHasNext()).isTrue();

		List<ConcertResponseDto> concerts = pageData.getContent();
		for (int i = 0; i < concerts.size(); i++) {
			ConcertResponseDto concert = concerts.get(i);
			Assertions.assertThat(concert.getName()).isEqualTo("콘서트 " + i);
			Assertions.assertThat(concert.getArtistName()).isEqualTo("아티스트 " + i);
			Assertions.assertThat(concert.getStatus()).isEqualTo(ConcertStatus.OPEN.name());
		}
	}


	@Test
	@DisplayName("예매 가능한 콘서트 목록을 페이징하여 조회한다")
	void getAvailableConcerts() throws Exception {
		// given
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
		List<ConcertResponseDto> mockConcerts = createMockConcertResponseDtos(10);

		PageResponse<ConcertResponseDto> responseDto = new PageResponse<>(
			mockConcerts,
			0,
			2,
			19,
			true
		);

		given(concertService.getAvailableReservationConcerts(any(Pageable.class)))
			.willReturn(responseDto);

		// when
		ResultActions resultActions = mockMvc.perform(
			get("/api/concerts/available")
				.param("page", "0")
				.param("size", "10")
				.param("sort", "createdAt,desc"));

		// then
		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
			.andReturn().getResponse();

		ApiResponse<PageResponse<ConcertResponseDto>> apiResponse = objectMapper.readValue(
			response.getContentAsString(),
			new TypeReference<ApiResponse<PageResponse<ConcertResponseDto>>>() {}
		);

		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");

		// PageResponse 객체의 내용 검증
		PageResponse<ConcertResponseDto> pageData = apiResponse.getData();
		Assertions.assertThat(pageData.getContent()).hasSize(10);
		Assertions.assertThat(pageData.getTotalElements()).isEqualTo(19);
		Assertions.assertThat(pageData.getCurrentPage()).isEqualTo(0);
		Assertions.assertThat(pageData.getTotalPages()).isEqualTo(2);
		Assertions.assertThat(pageData.isHasNext()).isTrue();
	}

	@Test
	@DisplayName("특정 콘서트 상세 정보를 조회한다")
	void getConcertDetail() throws Exception {
		// given
		Long concertId = 1L;
		ConcertDetailResponseDto responseDto = ConcertDetailResponseDto.builder()
			.id(concertId)
			.name("Shining Star 콘서트")
			.artistName("엑소")
			.status(ConcertStatus.OPEN.name())
			.startTime(LocalDateTime.now().toString())
			.endTime(LocalDateTime.now().plusHours(2).toString())
			.concertHallName("서울 콘서트홀")
			.concertHallAddress("서울시 강남구")
			.remainingSeat(100)
			.reservationStartTime(LocalDateTime.now().minusDays(1).toString())
			.reservationEndTime(LocalDateTime.now().plusDays(1).toString())
			.build();

		given(concertService.getConcertById(concertId))
			.willReturn(responseDto);

		// when
		ResultActions resultActions = mockMvc.perform(
			get("/api/concerts/{concertId}", concertId));

		// then
		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
			.andReturn().getResponse();

		ApiResponse<ConcertDetailResponseDto> apiResponse = objectMapper.readValue(
			response.getContentAsString(),
			new TypeReference<ApiResponse<ConcertDetailResponseDto>>() {}
		);

		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");

		ConcertDetailResponseDto concertDetail = apiResponse.getData();
		Assertions.assertThat(concertDetail.getId()).isEqualTo(concertId);
		Assertions.assertThat(concertDetail.getName()).isEqualTo("Shining Star 콘서트");
		Assertions.assertThat(concertDetail.getArtistName()).isEqualTo("엑소");
		Assertions.assertThat(concertDetail.getStatus()).isEqualTo(ConcertStatus.OPEN.name());
	}

	@Test
	@DisplayName("콘서트 좌석 정보를 페이징하여 조회한다")
	void getConcertSeats() throws Exception {
		// given
		Long concertId = 1L;
		Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "seatNumber"));

		List<ConcertSeatResponseDto> mockSeats = IntStream.range(0, 50)
			.mapToObj(i -> ConcertSeatResponseDto.builder()
				.id((long) i)
				.concertId(concertId)
				.seatNumber((i + 1))
				.build())
			.collect(Collectors.toList());

		PageResponse<ConcertSeatResponseDto> responseDto = new PageResponse<>(
			mockSeats,
			0,
			1,
			50,
			false
		);

		given(concertService.getConcertSeats(eq(concertId), any(Pageable.class)))
			.willReturn(responseDto);

		// when
		ResultActions resultActions = mockMvc.perform(
			get("/api/concerts/{concertId}/seats", concertId)
				.param("page", "0")
				.param("size", "50")
				.param("sort", "seatNumber,asc"));

		// then
		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
			.andReturn().getResponse();

		ApiResponse<PageResponse<ConcertSeatResponseDto>> apiResponse = objectMapper.readValue(
			response.getContentAsString(),
			new TypeReference<ApiResponse<PageResponse<ConcertSeatResponseDto>>>() {}
		);

		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");

		// PageResponse 객체의 내용 검증
		PageResponse<ConcertSeatResponseDto> pageData = apiResponse.getData();
		Assertions.assertThat(pageData.getContent()).hasSize(50);
		Assertions.assertThat(pageData.getTotalElements()).isEqualTo(50);
		Assertions.assertThat(pageData.getCurrentPage()).isEqualTo(0);
		Assertions.assertThat(pageData.getTotalPages()).isEqualTo(1);
		Assertions.assertThat(pageData.isHasNext()).isFalse();

		// 좌석 DTO 검증
		List<ConcertSeatResponseDto> seats = pageData.getContent();
		for (int i = 0; i < seats.size(); i++) {
			ConcertSeatResponseDto seat = seats.get(i);
			Assertions.assertThat(seat.getId()).isEqualTo(i);
			Assertions.assertThat(seat.getConcertId()).isEqualTo(concertId);
			Assertions.assertThat(seat.getSeatNumber()).isEqualTo((i + 1));
		}
	}
}
