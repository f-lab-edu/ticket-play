package com.flab.tiple.ticket.reservation.infrastructure.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@Configuration
@EnableAsync
@EnableScheduling
@EnableRetry
public class AsyncTimeoutConfig {

	// Timeout Executor 설정
	@Value("${spring.task.timeout.executor.core_pool_size:5}")
	private int timeoutCorePoolSize;

	@Value("${spring.task.timeout.executor.max_pool_size:20}")
	private int timeoutMaxPoolSize;

	@Value("${spring.task.timeout.executor.queue_capacity:200}")
	private int timeoutQueueCapacity;

	@Value("${spring.task.timeout.executor.keep_alive_seconds:60}")
	private int timeoutKeepAliveSeconds;

	@Value("${spring.task.timeout.executor.thread_name_prefix:timeout-}")
	private String timeoutThreadNamePrefix;

	@Value("${spring.task.timeout.executor.await_termination_seconds:30}")
	private int timeoutAwaitTerminationSeconds;

	// Event Executor 설정
	@Value("${spring.task.event.executor.core_pool_size:3}")
	private int eventCorePoolSize;

	@Value("${spring.task.event.executor.max_pool_size:10}")
	private int eventMaxPoolSize;

	@Value("${spring.task.event.executor.queue_capacity:100}")
	private int eventQueueCapacity;

	@Value("${spring.task.event.executor.thread_name_prefix:event-}")
	private String eventThreadNamePrefix;

	@Bean(name = "timeoutTaskExecutor")
	public TaskExecutor timeoutTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(timeoutCorePoolSize);
		executor.setMaxPoolSize(timeoutMaxPoolSize);
		executor.setQueueCapacity(timeoutQueueCapacity);
		executor.setKeepAliveSeconds(timeoutKeepAliveSeconds);
		executor.setThreadNamePrefix(timeoutThreadNamePrefix);
		executor.setAwaitTerminationSeconds(timeoutAwaitTerminationSeconds);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();
		return executor;
	}

	@Bean(name = "eventTaskExecutor")
	public TaskExecutor eventTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(eventCorePoolSize);
		executor.setMaxPoolSize(eventMaxPoolSize);
		executor.setQueueCapacity(eventQueueCapacity);
		executor.setThreadNamePrefix(eventThreadNamePrefix);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}

}