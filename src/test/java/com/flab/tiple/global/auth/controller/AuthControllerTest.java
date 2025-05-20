package com.flab.tiple.global.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.auth.controller.AuthController;
import com.flab.tiple.auth.dto.request.LoginRequestDto;
import com.flab.tiple.auth.dto.response.LoginResponseDto;
import com.flab.tiple.auth.service.AuthServiceImpl;
import com.flab.tiple.global.config.AbstractRestDocs;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.response.ApiResponse;
import com.flab.tiple.global.security.CustomUserDetailsService;
import com.flab.tiple.global.security.JwtTokenProvider;
import com.flab.tiple.global.security.SecurityConfig;
import com.flab.tiple.global.util.JwtTokenUtil;
import com.flab.tiple.member.exception.MemberNotFoundException;

//실제 내가 설정한 SecurityConfig환경내에서 테스트하기 위해 설정을 가져옴.
@WebMvcTest({AuthController.class, SecurityConfig.class})
public class AuthControllerTest extends AbstractRestDocs {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthServiceImpl authService;

	// SecurityConfig에 필요한 의존성 MockBean으로 추가
	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		//openssl 명령어로 랜덤 값 생성
		System.setProperty("jwt.secret", "00d3c5be72e5a5ab6cf053ddb4a016f9c6521718b01422fbfe83103211445dd100acb59223d0d67da44400c4f9ef9735d78c6c33c9950a46ca5a603d4c845e14");
	}

	@AfterEach
	void tearDown() {
		System.clearProperty("jwt.secret");
	}

	/**
	 * @WithMockUser의 역할
	 *   -> 인증된 사용자 시뮬레이션: 테스트 실행 시 Spring Security의 SecurityContext에 가상의 인증된 사용자를 설정합니다.
	 *   * 작동 방식
	 *       1.테스트 메서드가 실행되기 전에 SecurityContextHolder의 SecurityContext를 수정합니다.
	 *       2.지정된 사용자 정보로 UsernamePasswordAuthenticationToken을 생성합니다.
	 *       3.이 토큰을 SecurityContext에 설정합니다.
	 *       4.테스트 메서드가 실행됩니다.
	 *       5.테스트 후에는 SecurityContext가 초기화됩니다.
	 */
	@Test
	@DisplayName("로그인 성공 테스트")
	@WithMockUser
	void loginSuccess() throws Exception {
		// given
		LoginRequestDto requestDto = new LoginRequestDto("test@example.com", "password123");
		LoginResponseDto responseDto = new LoginResponseDto("test.jwt.token");

		given(authService.login(any(LoginRequestDto.class)))
			.willReturn(responseDto);

		//when
		ResultActions resultActions = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)));

		//then
		MockHttpServletResponse response = resultActions.andExpect(status().isOk())
			.andReturn().getResponse();

		ApiResponse<LoginResponseDto> apiResponse = objectMapper.readValue(
			response.getContentAsString(),
			new TypeReference<ApiResponse<LoginResponseDto>>() {}
		);

		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");
		Assertions.assertThat(apiResponse.getData()).usingRecursiveComparison().isEqualTo(responseDto);

	}


	@Test
	@DisplayName("로그인 실패 테스트 - 존재하지 않는 사용자")
	@WithMockUser
	void loginFailureUserNotFound() throws Exception {
		// given
		LoginRequestDto requestDto = new LoginRequestDto("nonexistent@example.com", "password123");

		given(authService.login(any(LoginRequestDto.class)))
			.willThrow(new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND, ErrorCode.MEMBER_NOT_FOUND.getDescription()));

		//when
		ResultActions resultActions = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)));

		//then
		MockHttpServletResponse response = resultActions
			.andExpect(status().isOk())  // HTTP 상태는 200이 반환됨
			.andReturn().getResponse();

		ApiResponse<?> apiResponse = objectMapper.readValue(
			response.getContentAsString(),
			new TypeReference<ApiResponse<?>>() {}
		);

		// API 응답의 status는 400을 확인
		Assertions.assertThat(apiResponse.getStatus()).isEqualTo(400);
		Assertions.assertThat(apiResponse.getMessage()).isEqualTo("유저를 찾을 수 없습니다");

		// 데이터 필드에 오류 코드와 메시지가 포함되어 있는지 확인
		Map<String, String> errorData = (Map<String, String>) apiResponse.getData();
		Assertions.assertThat(errorData).isNotNull();
		Assertions.assertThat(errorData.get("code")).isEqualTo("MEMBER_NOT_FOUND");
		Assertions.assertThat(errorData.get("message")).isEqualTo("유저를 찾을 수 없습니다");
	}

}
