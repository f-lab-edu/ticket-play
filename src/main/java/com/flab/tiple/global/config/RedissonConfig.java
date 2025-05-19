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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.flab.tiple.global.listner.RedisKeyExpirationListener;

@Configuration
@Profile("!test") // 테스트 프로파일이 아닐 때만 이 설정 사용
public class RedissonConfig {

	private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

	@Value("${spring.data.redis.host:127.0.0.1}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	@Value("${spring.data.redis.redisson.config.single-server-config.connection-minimum-idle-size:8}")
	private int redisMinimumIdleSize;

	@Value("${spring.data.redis.redisson.config.single-server-config.connection-pool-size:24}")
	private int redisConnectionPoolSize;

	@Value("${spring.data.redis.redisson.config.single-server-config.retry-attempts:3}")
	private int redisRetryAttempts;

	@Value("${spring.data.redis.redisson.config.single-server-config.retry-interval:1500}")
	private int redisRetryInterval;

	@Value("${spring.data.redis.redisson.config.single-server-config.subscriptions-per-connection:5}")
	private int redisSubscriptionsPerConnection;

	@Value("${spring.data.redis.redisson.config.single-server-config.subscription-connection-pool-size:50}")
	private int redisSubscriptionConnectionPoolSize;

	@Value("${spring.data.redis.redisson.config.single-server-config.timeout:3000}")
	private int redisTimeout;


	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		String redisAddress = "redis://" + redisHost + ":" + redisPort;
		log.info("Configuring Redisson with Redis address: {}", redisAddress);

		config.useSingleServer()
			.setAddress(redisAddress)
			.setConnectionMinimumIdleSize(redisMinimumIdleSize)    // 유휴 연결 수 증가
			.setConnectionPoolSize(redisConnectionPoolSize)          // 연결 풀 크기 증가
			.setRetryAttempts(redisRetryAttempts)                // 재시도 횟수 증가
			.setRetryInterval(redisRetryInterval)              // 재시도 간격 감소
			.setSubscriptionsPerConnection(redisSubscriptionsPerConnection)  // 연결당 구독 수
			.setSubscriptionConnectionPoolSize(redisSubscriptionConnectionPoolSize)  // 구독 연결 풀 크기
			.setTimeout(redisTimeout);                  // 타임아웃 설정

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

	/**
	 * StringRedisTemplate: Redis에 문자열(String) 기반 데이터를 저장하고 조회할 때 사용하는 클래스
	 */
	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(connectionFactory);
		return template;
	}

	/**
	 * 목적: Redis의 Key 만료 이벤트(expired) 를 감지해서 처리하는 리스너를 등록하는 설정입니다.
	 * param:
	 * connectionFactory:	Redis 연결 설정입니다.
	 * RedisKeyExpirationListener: 실제 만료 이벤트 처리 로직을 담당하는 리스너 클래스입니다.
	 * RedisMessageListenerContainer: Redis Pub/Sub 메시지를 리스닝하는 컨테이너입니다.
	 * __keyevent@*__:expired: Redis key가 만료되었을 때 발행되는 이벤트를 구독합니다.
	 * addMessageListener(...): 만료 이벤트가 발생하면 keyExpirationListener가 호출됩니다.
	 *
	 * 🔔 이 설정은 예를 들어, 대기열 TTL이 끝났을 때 알림을 보내거나 자동 취소 등의 후처리를 가능하게 해줍니다.
	 */
	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(
		RedisConnectionFactory connectionFactory,
		RedisKeyExpirationListener keyExpirationListener) {

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(keyExpirationListener, new PatternTopic("__keyevent@*__:expired"));

		return container;
	}
}
