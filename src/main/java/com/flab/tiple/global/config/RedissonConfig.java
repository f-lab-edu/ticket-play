package com.flab.tiple.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("!test") // 테스트 프로파일이 아닐 때만 이 설정 사용
public class RedissonConfig {

	private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

	@Value("${spring.data.redis.host}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		String redisAddress = "redis://" + redisHost + ":" + redisPort;
		log.info("Configuring Redisson with Redis address: {}", redisAddress);

		config.useSingleServer()
			.setAddress(redisAddress)
			.setConnectionMinimumIdleSize(20)    // 유휴 연결 수 증가
			.setConnectionPoolSize(64)          // 연결 풀 크기 증가
			.setRetryAttempts(5)                // 재시도 횟수 증가
			.setRetryInterval(500)              // 재시도 간격 감소
			.setSubscriptionsPerConnection(10)  // 연결당 구독 수
			.setSubscriptionConnectionPoolSize(50)  // 구독 연결 풀 크기
			.setTimeout(3000);                  // 타임아웃 설정

		return Redisson.create(config);
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
		return template;
	}
}