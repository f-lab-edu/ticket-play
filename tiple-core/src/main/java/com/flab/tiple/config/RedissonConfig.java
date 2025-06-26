package com.flab.tiple.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;



@Configuration
public class RedissonConfig {

	@Value("${spring.data.redis.host:redis}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	@Bean
	@ConditionalOnMissingBean
	public RedissonClient redissonClient() {
		Config config = new Config();
		String address = String.format("redis://%s:%d", redisHost, redisPort);
		config.useSingleServer()
			.setAddress(address)
			.setConnectTimeout(10000)
			.setTimeout(10000)
			.setRetryAttempts(5)
			.setRetryInterval(2000)
			.setPingConnectionInterval(30000);

		try {
			return Redisson.create(config);
		} catch (Exception e) {
			throw e;
		}
	}
}
