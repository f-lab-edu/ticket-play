package com.flab.tiple.infrastructure.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.flab.tiple.application.SseNotificationUseCase;
import com.flab.tiple.domain.repository.NotificationService;
import com.flab.tiple.domain.repository.SseConnectionRepository;
import com.flab.tiple.infrastructure.repository.SseConnectionRepositoryImpl;
import com.flab.tiple.infrastructure.service.SseNotificationServiceImpl;

@AutoConfiguration
@EnableConfigurationProperties(SseProperties.class)
@ComponentScan(basePackages = "com.flab.tiple")
public class SseAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SseConnectionRepository sseConnectionRepository() {
		return new SseConnectionRepositoryImpl();
	}

	@Bean
	@ConditionalOnMissingBean
	public NotificationService notificationService(
		SseConnectionRepository connectionRepository,
		SseProperties sseProperties) {
		return new SseNotificationServiceImpl(connectionRepository, sseProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public SseNotificationUseCase sseNotificationUseCase(NotificationService notificationService) {
		return new SseNotificationUseCase(notificationService);
	}
}