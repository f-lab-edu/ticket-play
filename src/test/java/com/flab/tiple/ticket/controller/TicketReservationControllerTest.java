// package com.flab.tiple.ticket.controller;
//
// import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyLong;
// import static org.mockito.BDDMockito.given;
// import static org.mockito.Mockito.when;
//
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
// import java.lang.reflect.Field;
// import java.time.LocalDateTime;
// import java.util.Collections;
// import java.util.List;
//
// import org.assertj.core.api.Assertions;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.context.annotation.ComponentScan;
// import org.springframework.context.annotation.FilterType;
// import org.springframework.context.annotation.Import;
// import org.springframework.http.MediaType;
// import org.springframework.mock.web.MockHttpServletResponse;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
// import org.springframework.test.util.ReflectionTestUtils;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.test.web.servlet.ResultActions;
//
// import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.flab.tiple.concert.dto.response.ConcertSeatInfo;
// import com.flab.tiple.concert.enums.ConcertSeatGrade;
// import com.flab.tiple.global.auth.aop.LoginCheckAspect;
// import com.flab.tiple.global.config.MockBeanConfig;
// import com.flab.tiple.global.config.TestSecurityConfig;
// import com.flab.tiple.global.response.ApiResponse;
// import com.flab.tiple.global.security.JwtTokenProvider;
// import com.flab.tiple.global.security.SecurityConfig;
// import com.flab.tiple.global.util.JwtTokenUtil;
// import com.flab.tiple.member.dto.response.MemberInfoDto;
// import com.flab.tiple.ticket.reservation.controller.TicketReservationController;
// import com.flab.tiple.ticket.reservation.dto.request.TicketReservationRequestDto;
// import com.flab.tiple.ticket.reservation.dto.response.TicketReservationInfoResponseDto;
// import com.flab.tiple.ticket.reservation.dto.response.TicketReservationResponseDto;
// import com.flab.tiple.ticket.reservation.enums.TicketProcessStatus;
// import com.flab.tiple.ticket.reservation.enums.TicketReservationStatus;
// import com.flab.tiple.ticket.reservation.service.TicketReservationService;
// import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingResponseDto;
//
// @WebMvcTest(
// 	controllers = TicketReservationController.class,
// 	excludeAutoConfiguration = {SecurityAutoConfiguration.class},
// 	excludeFilters = {
// 		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
// 	}
// )
// @Import({TestSecurityConfig.class, MockBeanConfig.class})
// public class TicketReservationControllerTest {
// 	@Autowired
// 	private MockMvc mockMvc;
//
// 	@Autowired
// 	private ObjectMapper objectMapper;
//
// 	@MockitoBean
// 	private TicketReservationService ticketReservationService;
//
// 	@MockitoBean
// 	private JwtTokenUtil jwtTokenUtil;
//
// 	@MockitoBean
// 	private JwtTokenProvider jwtTokenProvider;
//
// 	@Autowired
// 	private LoginCheckAspect loginCheckAspect;
//
//
// 	private TicketReservationRequestDto ticketReservationRequestDto;
// 	private TicketReservationInfoResponseDto ticketReservationInfoResponseDto;
// 	private TicketWaitingResponseDto ticketWaitingResponseDto;
// 	private Long memberId = 1L;
//
// 	@BeforeEach
// 	void setUp() {
// 		System.setProperty("jwt.secret", "00d3c5be72e5a5ab6cf053ddb4a016f9c6521718b01422fbfe83103211445dd100acb59223d0d67da44400c4f9ef9735d78c6c33c9950a46ca5a603d4c845e14");
// 		SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
//
// 		// 모의 인증 설정
// 		Authentication authentication = new UsernamePasswordAuthenticationToken(
// 			"testuser",
// 			"password",
// 			Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
// 		);
// 		SecurityContextHolder.getContext().setAuthentication(authentication);
//
// 		// LoginCheckAspect에서 getCurrentMemberId() 메소드의 반환값 설정
// 		// 이 부분이 중요: LoginCheckAspect의 동작을 모킹
// 		try {
// 			Field field = LoginCheckAspect.class.getDeclaredField("currentMemberId");
// 			field.setAccessible(true);
// 			ThreadLocal<Long> memberIdThreadLocal = (ThreadLocal<Long>) field.get(null);
// 			memberIdThreadLocal.set(1L);
// 		} catch (Exception e) {
// 			e.printStackTrace();
// 		}
//
//
// 		ticketReservationInfoResponseDto = TicketReservationInfoResponseDto.builder()
// 			.id(1L)
// 			.memberInfo(MemberInfoDto.builder()
// 				.email("test@example.com")
// 				.name("테스트 사용자")
// 				.build())
// 			.seatInfo(ConcertSeatInfo.builder()
// 				.id(1L)
// 				.seatNumber(1)
// 				.grade(ConcertSeatGrade.A)
// 				.build())
// 			.status(TicketReservationStatus.PENDING)
// 			.createdAt(LocalDateTime.now().toString())
// 			.build();
//
// 		// 샘플 요청 DTO 생성
// 		ticketReservationRequestDto = TicketReservationRequestDto.builder()
// 			.seatId(1L)
// 			.build();
//
// 		ticketWaitingResponseDto = TicketWaitingResponseDto.builder()
// 			.waitingNumber(5)
// 			.concertId(1L)
// 			.concertName("Test Concert")
// 			.status("WAITING")
// 			.build();
// 	}
//
// 	@Test
// 	@DisplayName("예약 요청 성공 - /api/ticket-reservations/request")
// 	void requestReservationPendingSuccess() throws Exception {
// 		// Given
// 		TicketReservationResponseDto<TicketReservationInfoResponseDto> responseDto =
// 			TicketReservationResponseDto.<TicketReservationInfoResponseDto>builder()
// 				.status(TicketProcessStatus.WAITING)
// 				.data(ticketReservationInfoResponseDto)
// 				.build();
//
// 		given(ticketReservationService.requestReservation(any(TicketReservationRequestDto.class), eq(1L)))
// 			.willReturn(responseDto);
//
//
// 		// When
// 		ResultActions resultActions = mockMvc.perform(post("/api/ticket-reservations/request")
// 			.contentType(MediaType.APPLICATION_JSON)
// 			.content(objectMapper.writeValueAsString(ticketReservationRequestDto)));
//
// 		// then
// 		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
// 			.andReturn().getResponse();
//
// 		// 응답 내용 출력
//
// 		ApiResponse<TicketReservationResponseDto<TicketReservationInfoResponseDto>> apiResponse = objectMapper.readValue(
// 			response.getContentAsString(),
// 			new TypeReference<ApiResponse<TicketReservationResponseDto<TicketReservationInfoResponseDto>>>() {}
// 		);
//
//
// 		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
// 		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");
// 		Assertions.assertThat(apiResponse.getData()).isNotNull(); // null 체크 추가
// 		Assertions.assertThat(apiResponse.getData())
// 			.usingRecursiveComparison()
// 			.ignoringFields("createdAt")
// 			.isEqualTo(responseDto);
//
// 		Long currentMemberId = LoginCheckAspect.getCurrentMemberId();
// 		assertThat(currentMemberId).isEqualTo(1L);
//
// 	}
//
// 	@Test
// 	@DisplayName("티켓 예약 요청 - 웨이팅 등록 케이스")
// 	void requestReservation_WaitingRegistration() throws Exception {
// 		// Given
//
// 		TicketReservationResponseDto<TicketWaitingResponseDto> responseDto =
// 			TicketReservationResponseDto.<TicketWaitingResponseDto>builder()
// 				.status(TicketProcessStatus.WAITING)
// 				.data(ticketWaitingResponseDto)
// 				.build();
//
// 		// Service 모의 설정
// 		when(ticketReservationService.requestReservation(any(TicketReservationRequestDto.class), eq(memberId)))
// 			.thenReturn(responseDto);
//
// 		ResultActions resultActions = mockMvc.perform(post("/api/ticket-reservations/request")
// 			.contentType(MediaType.APPLICATION_JSON)
// 			.content(objectMapper.writeValueAsString(ticketReservationRequestDto)));
//
// 		// then
// 		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
// 			.andReturn().getResponse();
//
// 		// 응답 내용 출력
//
// 		ApiResponse<TicketReservationResponseDto<TicketWaitingResponseDto>> apiResponse = objectMapper.readValue(
// 			response.getContentAsString(),
// 			new TypeReference<ApiResponse<TicketReservationResponseDto<TicketWaitingResponseDto>>>() {}
// 		);
//
//
// 		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
// 		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");
// 		Assertions.assertThat(apiResponse.getData()).isNotNull(); // null 체크 추가
// 		Assertions.assertThat(apiResponse.getData())
// 			.usingRecursiveComparison()
// 			.ignoringFields("createdAt")
// 			.isEqualTo(responseDto);
//
//
// 	}
//
//
// 	@Test
// 	@DisplayName("예약 승인 성공 - /api/ticket-reservations/1/approve")
// 	void approveReservationSuccess() throws Exception {
// 		// Given
// 		ReflectionTestUtils.setField(ticketReservationInfoResponseDto, "status", TicketReservationStatus.APPROVED);
// 		when(ticketReservationService.approveReservation(anyLong(), anyLong()))
// 			.thenReturn(ticketReservationInfoResponseDto);
//
//
// 		// When
// 		ResultActions resultActions = mockMvc.perform(post("/api/ticket-reservations/1/approve")
// 			.contentType(MediaType.APPLICATION_JSON)
// 			.content(objectMapper.writeValueAsString(ticketReservationRequestDto)));
//
// 		// then
// 		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
// 			.andReturn().getResponse();
//
//
// 		ApiResponse<TicketReservationInfoResponseDto> apiResponse = objectMapper.readValue(
// 			response.getContentAsString(),
// 			new TypeReference<ApiResponse<TicketReservationInfoResponseDto>>() {}
// 		);
//
// 		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
// 		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");
// 		Assertions.assertThat(apiResponse.getData()).usingRecursiveComparison().isEqualTo(
// 			ticketReservationInfoResponseDto);
// 	}
//
//
// 	@Test
// 	@DisplayName("예약 취소 성공 - /api/ticket-reservations/1/cancel")
// 	void cancelReservationSuccess() throws Exception {
// 		// Given
// 		ReflectionTestUtils.setField(ticketReservationInfoResponseDto, "status", TicketReservationStatus.CANCELLED);
// 		when(ticketReservationService.cancelReservation(anyLong(), anyLong()))
// 			.thenReturn(ticketReservationInfoResponseDto);
//
// 		// When
// 		ResultActions resultActions = mockMvc.perform(delete("/api/ticket-reservations/1/cancel")
// 				.contentType(MediaType.APPLICATION_JSON)
// 				.content(objectMapper.writeValueAsString(ticketReservationRequestDto)));
//
// 		// then
// 		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
// 			.andReturn().getResponse();
//
//
// 		ApiResponse<TicketReservationInfoResponseDto> apiResponse = objectMapper.readValue(
// 			response.getContentAsString(),
// 			new TypeReference<ApiResponse<TicketReservationInfoResponseDto>>() {}
// 		);
//
// 		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
// 		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");
// 		Assertions.assertThat(apiResponse.getData()).usingRecursiveComparison().isEqualTo(
// 			ticketReservationInfoResponseDto);
// 	}
//
// 	@Test
// 	@DisplayName("예약 목록 조회 - /api/ticket-reservations/my-reservations")
// 	void getMemberReservationsSuccess() throws Exception {
// 		// Given
// 		List<TicketReservationInfoResponseDto> reservations = List.of(ticketReservationInfoResponseDto);
// 		when(ticketReservationService.getMemberReservations(anyLong()))
// 			.thenReturn(reservations);
//
// 		// When & Then
// 		ResultActions resultActions = mockMvc.perform(get("/api/ticket-reservations/my-reservations")
// 			.contentType(MediaType.APPLICATION_JSON));
//
// 		// then
// 		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
// 			.andReturn().getResponse();
//
//
// 		ApiResponse<List<TicketReservationInfoResponseDto>> apiResponse = objectMapper.readValue(
// 			response.getContentAsString(),
// 			new TypeReference<ApiResponse<List<TicketReservationInfoResponseDto>>>() {}
// 		);
//
// 		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
// 		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");
// 	}
//
// }
