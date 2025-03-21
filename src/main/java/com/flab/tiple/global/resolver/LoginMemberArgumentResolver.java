package com.flab.tiple.global.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.flab.tiple.global.auth.aop.LoginCheck;
import com.flab.tiple.global.auth.aop.LoginCheckAspect;

@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		// @LoginCheck 어노테이션이 붙어있고, Long 타입인 파라미터를 지원
		return parameter.hasParameterAnnotation(LoginCheck.class)
			&& parameter.getParameterType().equals(Long.class);
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) throws Exception {
		// LoginCheckAspect에서 설정한 현재 회원 ID 가져오기
		return LoginCheckAspect.getCurrentMemberId();
	}
}

