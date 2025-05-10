package com.flab.tiple.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.flab.tiple.ticket.reservation.initializer.StatusTransitionJsonInitializer;

@Configuration
@Profile("test")
public class TestConfiguration {

	/**
	 * 테스트용 StatusTransitionJsonInitializer 빈 생성
	 * 테스트 환경에서 PostConstruct가 실행될 수 있도록 함
	 */
	@Bean
	public StatusTransitionJsonInitializer statusTransitionJsonInitializer() {
		return new StatusTransitionJsonInitializer();
	}
}