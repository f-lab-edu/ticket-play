package com.flab.tiple.global.config;

import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test") // 테스트 환경에서만 이 구성 활성화
public class RedissonConfig {

	@Bean
	@Primary
	public RedissonClient redissonClient() {
		// Redis 클라이언트를 모킹하여 테스트 중에 실제 연결이 필요하지 않도록 함
		return Mockito.mock(RedissonClient.class);
	}
}