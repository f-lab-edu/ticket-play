package com.flab.tiple.global.security;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.global.exception.TokenException;
import com.flab.tiple.global.response.ApiResponse;
import com.flab.tiple.global.util.JwtTokenUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OncePerRequestFilter
 * 	특징: 요청당 한 번만 실행을 보장
 * 	장점: 여러 서블릿 디스패처 타입에서 중복 실행 방지
 *
 * 	사용이유
 *		1.중복 실행 방지: JWT 인증 로직은 한 요청에서 반드시 한 번만 실행되어야 합니다. OncePerRequestFilter는 forward, include, async 요청에서도 한 번만 실행을 보장합니다.
 * 		2.실제 권장 사례: '스프링 시큐리티 공식 문서'와 참조 구현에서도 인증 필터로 OncePerRequestFilter를 사용합니다.
 * 		3.필터 체인 관리: REQUEST 타입의 요청에서만 기본적으로 실행되어 불필요한 오버헤드를 줄입니다.
 *
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final CustomUserDetailsService userDetailsService;
	private final JwtTokenUtil jwtTokenUtil;

	/**
	 * doFilterInternal: doFilterInternal호출시  OncePerRequestFilter의 doFilter 메서드를 호출
	 *
	 * OncePerRequestFilter의 doFilter 의 작업순서
	 *		1. 요청/응답을 HttpServletRequest/HttpServletResponse로 캐스팅
	 *		2. 이미 실행되었는지 확인 (요청 속성 체크)
	 *		3. 필터를 건너뛰어야 하는지 확인
	 *		-> 이 작업순서로 요청 당 한 번만 실행할 수 있도록 보장해줌.
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		try {
			String jwt = jwtTokenUtil.getJwtFromRequest(request);

			if (StringUtils.hasText(jwt)) {
				// validateToken이 예외를 던지지 않으면 토큰은 유효함
				jwtTokenProvider.validateToken(jwt);

				String email = jwtTokenProvider.getEmailFromToken(jwt);
				List<String> roles = jwtTokenProvider.getRolesFromToken(jwt);

				UserDetails userDetails = userDetailsService.loadUserByUsername(email);

				Collection<SimpleGrantedAuthority> authorities = roles.stream()
					.map(SimpleGrantedAuthority::new)
					.collect(Collectors.toList());

				/**
				 * UsernamePasswordAuthenticationToken: 사용자의 인증 정보를 담는 객체. 인증 전과 인증 후 두 가지 상태로 사용
				 * 	 생성자에 필요한 정보:
				 * 	 	1. principal: 사용자를 식별하는 정보입니다.
				 * 			인증 전: 일반적으로 사용자 이름(문자열)
				 * 			인증 후: UserDetails 객체(사용자의 모든 정보)
				 * 		2. credentials: 인증에 필요한 자격 증명입니다.
				 * 			일반적으로 비밀번호
				 * 			인증 후에는 보안을 위해 null로 설정하는 경우가 많음
				 * 		3. authorities: 사용자의 권한 목록입니다. -> SimpleGrantedAuthority 객체의 컬렉션
				 *
				 *	인증 전 (자격 증명 검증이 필요한 상태): new UsernamePasswordAuthenticationToken(username, password);
				 *	인증 후 (검증완료) : new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
				 *
				 */
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					userDetails, null, authorities);
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (TokenException e) {
			// 토큰 예외 발생 시 처리
			SecurityContextHolder.clearContext(); // 컨텍스트 초기화

			// 직접 응답 작성
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setStatus(e.getErrorCode().getStatus());

			ApiResponse<String> errorResponse = ApiResponse.error(
				e.getErrorCode(), e.getMessage(), null);

			response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
			return; // 필터 체인 중단
		} catch (Exception ex) {
			log.error("JWT 인증 처리 중 오류 발생", ex);
		}

		filterChain.doFilter(request, response);
	}

}
