package com.flab.tiple.global.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.flab.tiple.global.auth.dto.request.LoginRequestDto;
import com.flab.tiple.global.auth.dto.response.LoginResponseDto;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.security.JwtTokenProvider;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.exception.MemberNotFoundException;
import com.flab.tiple.member.repository.MemberRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService{
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final MemberRepository memberRepository;

	@Override
	public LoginResponseDto login(LoginRequestDto loginRequestDto) {
		/**
		 * 1.인증 처리
		 * authenticationManager가 사용자 인증 요청을 처리하고 검증
		 *   성공시 -> Authentication객체 반환
		 *   실패시 -> AuthenticationException
		 */
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(
				loginRequestDto.getEmail(),
				loginRequestDto.getPassword()
			)
		);

		// 2. SecurityContext에 인증 정보 저장
		SecurityContextHolder.getContext().setAuthentication(authentication);

		// 3. JWT 토큰 생성
		String jwt = jwtTokenProvider.generateToken(
			loginRequestDto.getEmail(),
			authentication.getAuthorities()
		);

		// 4. 사용자 정보 조회
		Member member = memberRepository.findByEmail(loginRequestDto.getEmail())
			.orElseThrow(() -> new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND,ErrorCode.MEMBER_NOT_FOUND.getDescription()));

		// 5. 응답 DTO 생성
		return new LoginResponseDto(
			jwt
		);
	}


}
