package com.flab.tiple.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.response.ApiResponse;
import com.flab.tiple.global.util.JwtTokenUtil;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * @EnableWebSecurity : Spring Security 설정을 활성화하는 어노테이션
 *
 * SecurityFilterChain을 찾아 FilterChainProxy에 등록
 * Spring Security의 필터 체인을 구성
 *
 * SecurityFilterChain : 하나의 HTTP 요청을 처리하는 데 필요한 Spring Security 필터들의 집합
 *
 * 동작 방식:
 *   1. HTTP 요청이 서버에 도달하면 FilterChainProxy가 이를 가로챔
 *   2. FilterChainProxy는 등록된 모든 SecurityFilterChain의 matches() 메서드를 호출
 *   3. 첫 번째로 매칭되는 SecurityFilterChain이 선택됨
 *   4. 선택된 SecurityFilterChain의 모든 필터가 순서대로 실행됨
 *   5. 모든 필터를 통과한 요청은 컨트롤러로 전달됨
 *
 * FilterChainProxy : 모든 보안 필터들을 관리하고 HTTP 요청을 적절한 보안 필터 체인으로 라우팅
 * FilterChainProxy의 역할
 *  1. 중앙 진입점: Spring Security의 모든 필터 처리에 대한 단일 진입점 역할
 *  2. 필터 체인 관리: 여러 SecurityFilterChain 인스턴스를 관리
 *  3. 요청 라우팅: HTTP 요청을 적절한 필터 체인으로 전달
 *  4. 디버깅 및 로깅: 보안 관련 디버깅 정보 제공
 *
 * 동작 방식
 *  1. 웹 요청이 서버에 도달
 *  2. 서블릿 컨테이너의 필터 체인을 거치는 중 FilterChainProxy에 도달
 *  3. FilterChainProxy는 등록된 모든 SecurityFilterChain을 검사하여 요청 URL과 매칭되는 첫 번째 체인을 선택
 *  4. 선택된 SecurityFilterChain의 필터들을 순서대로 실행
 *  5. 모든 보안 필터를 통과한 후, 남은 서블릿 필터 체인으로 요청 전달
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomUserDetailsService userDetailsService;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtTokenUtil jwtTokenUtil;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			//CSRF(Cross-Site Request Forgery)
			.csrf(csrf -> csrf.disable()) // REST API에서는 CSRF 보호가 필요 없음
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/members/signup", "/api/auth/login").permitAll() // 로그인 엔드포인트 추가
				.anyRequest().authenticated()
			)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) -> {
					response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

					ApiResponse<String> errorResponse = ApiResponse.error(
						ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDescription(), null);
					response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
				})
			)
			// HTTP Basic 인증 비활성화 (JWT 사용할 것이므로)
			.httpBasic(httpBasic -> httpBasic.disable())
			// 세션 관리 설정 (REST API는 일반적으로 무상태)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			);

		http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, jwtTokenUtil),
			UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(
		AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService); // CustomUserDetailsService 연결
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}
}