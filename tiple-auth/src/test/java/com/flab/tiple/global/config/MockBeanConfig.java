package com.flab.tiple.global.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.flab.tiple.aop.login.LoginCheckAspect;

@TestConfiguration
public class MockBeanConfig {

	@Bean
	@Primary
	public LoginCheckAspect loginCheckAspect() {
		// Mockito를 사용하여 LoginCheckAspect를 모킹
		return Mockito.mock(LoginCheckAspect.class);
	}

}
