package com.flab.tiple.waiting.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.global.auth.aop.LoginCheckAspect;
import com.flab.tiple.global.config.MockBeanConfig;
import com.flab.tiple.global.config.TestSecurityConfig;
import com.flab.tiple.global.response.ApiResponse;
import com.flab.tiple.global.security.JwtTokenProvider;
import com.flab.tiple.global.security.SecurityConfig;
import com.flab.tiple.global.util.JwtTokenUtil;
import com.flab.tiple.waiting.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.waiting.dto.response.TicketWaitingInfoResponseDto;
import com.flab.tiple.waiting.enums.TicketWaitingStatus;
import com.flab.tiple.waiting.service.TicketWaitingService;

@WebMvcTest(
	controllers = TicketWaitingController.class,
	excludeAutoConfiguration = {SecurityAutoConfiguration.class},
	excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
	}
)
@Import({TestSecurityConfig.class, MockBeanConfig.class})
public class TicketWaitingControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private TicketWaitingService ticketWaitingService;

	@MockitoBean
	private JwtTokenUtil jwtTokenUtil;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private LoginCheckAspect loginCheckAspect;

	private TicketWaitingCancelResponseDto ticketWaitingCancelResponseDto;
	private TicketWaitingInfoResponseDto ticketWaitingInfoResponseDto;

	private Long waitingId = 1L;
	private Long memberId = 1L;

	@BeforeEach
	void setUp() {
		System.setProperty("jwt.secret", "00d3c5be72e5a5ab6cf053ddb4a016f9c6521718b01422fbfe83103211445dd100acb59223d0d67da44400c4f9ef9735d78c6c33c9950a46ca5a603d4c845e14");
		SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());

		// 모의 인증 설정
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			"testuser",
			"password",
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		// LoginCheckAspect에서 getCurrentMemberId() 메소드의 반환값 설정
		// 이 부분이 중요: LoginCheckAspect의 동작을 모킹
		try {
			Field field = LoginCheckAspect.class.getDeclaredField("currentMemberId");
			field.setAccessible(true);
			ThreadLocal<Long> memberIdThreadLocal = (ThreadLocal<Long>) field.get(null);
			memberIdThreadLocal.set(1L);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ticketWaitingCancelResponseDto = TicketWaitingCancelResponseDto.builder()
			.id(1L)
			.status(TicketWaitingStatus.CANCELED)
			.deletedAt(LocalDateTime.now().toString())
			.build();

		ticketWaitingInfoResponseDto = TicketWaitingInfoResponseDto.builder()
			.waitingNumber(1)
			.concertId(1L)
			.concertName("콘서트명")
			.status("status")
			.build();
	}

	@Test
	@DisplayName("티켓 waiting 취소 - /api/ticket-waiting/{waitingId}/cancel")
	void cancelWaitingSuccess() throws Exception {
		// Given
		when(ticketWaitingService.cancelWaiting(anyLong(), anyLong())).thenReturn(ticketWaitingCancelResponseDto);

		// When
		ResultActions resultActions = mockMvc.perform(delete("/api/ticket-waiting/1/cancel")
			.contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(ticketWaitingCancelResponseDto)));

		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
			.andReturn().getResponse();

		ApiResponse<TicketWaitingCancelResponseDto> apiResponse = objectMapper.readValue(
			response.getContentAsString(),
			new TypeReference<ApiResponse<TicketWaitingCancelResponseDto>>() {}
		);

		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");
		Assertions.assertThat(apiResponse.getData()).usingRecursiveComparison().isEqualTo(
			ticketWaitingCancelResponseDto);

	}

	@Test
	@DisplayName("내 waiting목록 조회 - /api/ticket-waiting/my-waiting")
	void getMemberWaitingListSuccess() throws Exception {
		// Given
		List<TicketWaitingInfoResponseDto> waitings = List.of(ticketWaitingInfoResponseDto);
		when(ticketWaitingService.getMemberWaitingList(anyLong())).thenReturn(waitings);

		// When
		ResultActions resultActions = mockMvc.perform(get("/api/ticket-waiting/my-waiting")
			.contentType(MediaType.APPLICATION_JSON));

		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
			.andReturn().getResponse();

		ApiResponse<List<TicketWaitingInfoResponseDto>> apiResponse = objectMapper.readValue(
			response.getContentAsString(),
			new TypeReference<ApiResponse<List<TicketWaitingInfoResponseDto>>>() {}
		);

		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");
	}
}
