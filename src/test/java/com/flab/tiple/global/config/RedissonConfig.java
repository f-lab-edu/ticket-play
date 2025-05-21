package com.flab.tiple.global.config;

import java.util.Collections;
import java.util.HashSet;

import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.mockito.Mockito;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.convert.RedisCustomConversions;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.flab.tiple.global.listener.RedisKeyExpirationListener;
import com.flab.tiple.global.util.RedissionLockExecutor;
import com.flab.tiple.ticket.reservation.domain.repository.TicketReservationRedisRepository;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRedisRepository;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Profile("test") // 테스트 프로파일에서만 이 설정 사용
@Slf4j
public class RedissonConfig {

	@Bean
	@Primary
	public RedissonClient redissonClient() {
		log.info("Creating mock RedissonClient for test environment");
		return Mockito.mock(RedissonClient.class);
	}
	@Bean
	@Primary
	public RedisConnectionFactory redisConnectionFactory() {
		log.info("Creating mock RedisConnectionFactory for test environment");
		return Mockito.mock(RedisConnectionFactory.class);
	}

	@Bean
	@Primary
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		log.info("Creating mock RedisTemplate for test environment");
		RedisTemplate<String, Object> template = Mockito.mock(RedisTemplate.class);

		// 기본 operations 모킹
		ValueOperations<String, Object> valueOps = Mockito.mock(ValueOperations.class);
		HashOperations<String, Object, Object> hashOps = Mockito.mock(HashOperations.class);
		ListOperations<String, Object> listOps = Mockito.mock(ListOperations.class);
		SetOperations<String, Object> setOps = Mockito.mock(SetOperations.class);
		ZSetOperations<String, Object> zSetOps = Mockito.mock(ZSetOperations.class);

		Mockito.when(template.opsForValue()).thenReturn(valueOps);
		Mockito.when(template.opsForHash()).thenReturn(hashOps);
		Mockito.when(template.opsForList()).thenReturn(listOps);
		Mockito.when(template.opsForSet()).thenReturn(setOps);
		Mockito.when(template.opsForZSet()).thenReturn(zSetOps);

		// keys 메서드가 빈 결과를 반환하도록 설정
		Mockito.when(template.keys(Mockito.anyString())).thenReturn(new HashSet<>());

		return template;
	}

	@Bean
	@Primary
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
		log.info("Creating mock StringRedisTemplate for test environment");
		StringRedisTemplate template = Mockito.mock(StringRedisTemplate.class);

		// 기본 operations 모킹
		ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
		HashOperations<String, Object, Object> hashOps = Mockito.mock(HashOperations.class);
		ListOperations<String, String> listOps = Mockito.mock(ListOperations.class);
		SetOperations<String, String> setOps = Mockito.mock(SetOperations.class);
		ZSetOperations<String, String> zSetOps = Mockito.mock(ZSetOperations.class);

		Mockito.when(template.opsForValue()).thenReturn(valueOps);
		Mockito.when(template.opsForHash()).thenReturn(hashOps);
		Mockito.when(template.opsForList()).thenReturn(listOps);
		Mockito.when(template.opsForSet()).thenReturn(setOps);
		Mockito.when(template.opsForZSet()).thenReturn(zSetOps);

		// increment 메서드가 기본값을 반환하도록 설정
		Mockito.when(valueOps.increment(Mockito.anyString())).thenReturn(1L);

		return template;
	}

	@Bean
	@Primary
	public RedisMessageListenerContainer redisMessageListenerContainer(
		RedisConnectionFactory connectionFactory,
		RedisKeyExpirationListener keyExpirationListener) {

		log.info("Creating mock RedisMessageListenerContainer for test environment");
		RedisMessageListenerContainer container = Mockito.mock(RedisMessageListenerContainer.class);

		// 필요한 경우 메서드 모킹 추가

		return container;
	}

	@Bean
	@Primary
	public TicketWaitingRedisRepository ticketWaitingRedisRepository() {
		log.info("Creating mock TicketWaitingRedisRepository for test environment");
		return Mockito.mock(TicketWaitingRedisRepository.class);
	}

	@Bean
	@Primary
	public TicketReservationRedisRepository ticketReservationRedisRepository() {
		log.info("Creating mock TicketReservationRedisRepository for test environment");
		return Mockito.mock(TicketReservationRedisRepository.class);
	}

	@Bean
	@Primary
	public RedissionLockExecutor redissionLockExecutor() {
		log.info("Creating mock RedissionLockExecutor for test environment");
		RedissionLockExecutor executor = Mockito.mock(RedissionLockExecutor.class);

		// execute 메서드가 제공된 runnable을 실행하도록 모킹
		Mockito.doAnswer(invocation -> {
			String lockName = invocation.getArgument(0);
			long waitTime = invocation.getArgument(1);
			long leaseTime = invocation.getArgument(2);
			Runnable runnable = invocation.getArgument(3);

			// 실제로 runnable 실행
			runnable.run();
			return null;
		}).when(executor).execute(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(Runnable.class));

		return executor;
	}

	// 테스트 환경에서 필요한 추가 빈 설정

	// Redis Repository 활성화/비활성화 설정
	@Bean
	@Primary
	public RedisCustomConversions redisCustomConversions() {
		return new RedisCustomConversions(Collections.emptyList());
	}
}