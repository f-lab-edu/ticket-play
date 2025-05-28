package com.flab.tiple.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.flab.tiple.auth.application.dto.request.LoginRequestDto;
import com.flab.tiple.auth.application.dto.response.LoginResponseDto;
import com.flab.tiple.auth.application.service.AuthServiceImpl;
import com.flab.tiple.entity.member.Member;
import com.flab.tiple.entity.member.enums.RoleEnum;
import com.flab.tiple.repository.MemberRepository;
import com.flab.tiple.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
	@Mock
	private MemberRepository memberRepository;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@InjectMocks
	private AuthServiceImpl authService;

	private Member testMember;
	private LoginRequestDto loginRequestDto;
	private Authentication authentication;

	@BeforeEach
	void setUp() {
		testMember = Member.builder()
			.email("test@example.com")
			.name("테스트 사용자")
			.password("encodedPassword")
			.role(RoleEnum.ROLE_USER)
			.build();

		loginRequestDto = new LoginRequestDto("test@example.com", "password123");

		// 인증 객체 모킹 -> 인증완료된 userToken객체 생성
		SimpleGrantedAuthority authority = new SimpleGrantedAuthority(RoleEnum.ROLE_USER.name());
		authentication = new UsernamePasswordAuthenticationToken(
			"test@example.com", null, Collections.singleton(authority));

		//openssl 명령어로 랜덤 값 생성
		System.setProperty("jwt.secret", "00d3c5be72e5a5ab6cf053ddb4a016f9c6521718b01422fbfe83103211445dd100acb59223d0d67da44400c4f9ef9735d78c6c33c9950a46ca5a603d4c845e14");
	}

	@AfterEach
	void tearDown() {
		// 테스트 후 시스템 프로퍼티 초기화
		System.clearProperty("jwt.secret");
	}


	@Test
	@DisplayName("로그인 성공")
	void loginSuccess(){
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenReturn(authentication);

		when(jwtTokenProvider.generateToken(anyString(), any(), any()))
			.thenReturn("test.jwt.token");

		when(memberRepository.findByEmail("test@example.com"))
			.thenReturn(Optional.of(testMember));

		// when
		LoginResponseDto result = authService.login(loginRequestDto);

		// then
		/**
		 * assertNotNull(result)
		 *   목적: result 변수가 null이 아닌지 확인.
		 *   동작: result가 null이면 테스트가 실패하고, null이 아니면 통과.
		 */
		assertNotNull(result);

		//세부검증
		assertEquals("test.jwt.token", result.getToken());
	}

}
