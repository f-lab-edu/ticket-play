package com.flab.tiple.global.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.flab.tiple.auth.exception.JwtUnauthorizedException;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.security.JwtTokenProvider;
import com.flab.tiple.global.util.JwtTokenUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

/**
 * @Aspect
 *   여러곳에서 반복적으로 사용되는 로직(횡단 관심사(Cross-cutting Concern))을 모듈화
 *   스프링에서는 @EnableAspectJAutoProxy 설정을 통해 스프링 AOP 프록시 생성
 *   프록시 메커니즘을 통해 타겟 메서드 실행 전후로 추가 로직 삽입
 *
 * @Around("@annotation(loginCheck)")
 *   특정 어노테이션(@LoginCheck)이 붙은 메서드에 대해 전체 메서드 실행을 감싸는(Wrapping) 어드바이스
 *   메서드 실행 전, 후, 예외 발생 시 모든 시점에 개입 가능
 *   메커니즘:
 *   a. 대상 메서드 호출 전 인증/인가 로직 수행
 *   b. 조건 충족 시 원본 메서드 실행
 *   c. 조건 불충족 시 예외 발생
 *
 * Spring AOP에서 @Around는 메서드 호출을 가로채는 프록시를 생성.
 * 즉, @LoginCheck 애너테이션이 붙은 메서드가 호출될 때,
 * 프록시가 먼저 실행되고, 그 안에서 checkLoginStatus()를 실행한 후, joinPoint.proceed()를 통해 원래 메서드를 실행하는 구조
 */
@Aspect
@Component
@AllArgsConstructor
public class LoginCheckAspect {
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtTokenUtil jwtTokenUtil;

	// 현재 로그인된 회원의 ID를 저장할 ThreadLocal
	private static final ThreadLocal<Long> currentMemberId = new ThreadLocal<>();

	// 현재 로그인된 회원 ID를 가져오는 static 메서드 (서비스나 다른 곳에서 사용)
	public static Long getCurrentMemberId() {
		return currentMemberId.get();
	}

	/**
	 * ProceedingJoinPoint의 역할
	 * AOP가 가로챈 메서드에 대한 정보를 제공함
	 * 가로챈 메서드를 실행할지 말지 결정할 수 있음 (proceed() 호출 여부)
	 * 메서드 실행 전후에 추가 로직을 삽입할 수 있음
	 */
	@Around("@annotation(loginCheck)")
	public Object checkLoginStatus(ProceedingJoinPoint joinPoint, LoginCheck loginCheck) throws Throwable {
		try {
			// 로그인 필수 설정 확인
			if (loginCheck.required()) {
				// 현재 인증 컨텍스트에서 인증 정보 가져오기
				Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

				// 인증 정보 검증
				// 인증 객체 자체가 존재하지 않거나 인증되지 않은 사용자일경우 예외발생
				if (authentication == null ||
					!authentication.isAuthenticated()) {
					throw new JwtUnauthorizedException(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDescription());
				}

				// 현재 HTTP 요청 가져오기
				HttpServletRequest request =
					((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes())
						.getRequest();

				// 토큰 추출 및 검증
				String token = jwtTokenUtil.getJwtFromRequest(request);

				// 토큰 유효성 검사
				if (token == null || !jwtTokenProvider.validateToken(token)) {
					throw new JwtUnauthorizedException(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDescription());
				}

				// 토큰에서 회원 ID 추출 (JwtTokenProvider에 이 기능 추가 필요)
				Long memberId = jwtTokenProvider.getMemberIdFromToken(token);

				// ThreadLocal에 회원 ID 저장
				currentMemberId.set(memberId);
			}

			//원래 메서드 호출
			return joinPoint.proceed();
		} finally {
			// 스레드 로컬 초기화 (메모리 누수 방지)
			currentMemberId.remove();
		}
	}
}