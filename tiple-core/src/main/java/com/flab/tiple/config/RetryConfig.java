package com.flab.tiple.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableRetry
public class RetryConfig {

	private final int initialInterval = 1000;
	private final int maxInterval = 10000;
	private final double multiplier = 2.0;
	private final int maxAttempts = 3;

	@Bean
	public RetryTemplate reservationRetryTemplate() {
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(initialInterval); // 첫 재시도 대기 시간 (1초)
		backOffPolicy.setMultiplier(multiplier);       // 재시도마다 대기 시간 2배 증가
		backOffPolicy.setMaxInterval(maxInterval);    // 최대 대기 시간 (10초)

		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(maxAttempts);          // 최대 3번 시도

		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setBackOffPolicy(backOffPolicy);
		retryTemplate.setRetryPolicy(retryPolicy);

		return retryTemplate;
	}
}