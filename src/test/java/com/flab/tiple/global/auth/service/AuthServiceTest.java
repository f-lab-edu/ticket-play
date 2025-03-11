package com.flab.tiple.global.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;

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

import com.flab.tiple.global.auth.dto.request.LoginRequestDto;
import com.flab.tiple.global.auth.dto.response.LoginResponseDto;
import com.flab.tiple.global.security.JwtTokenProvider;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.enums.RoleEnum;
import com.flab.tiple.member.repository.MemberRepository;

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
	}

	@Test
	@DisplayName("로그인 성공")
	void loginSuccess(){
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenReturn(authentication);

		when(jwtTokenProvider.generateToken(anyString(), any()))
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
