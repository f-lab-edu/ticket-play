package com.flab.tiple.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ticket.sse")
public class SseProperties {
	private Long timeout = 60L * 1000 * 60; // 60분 기본값
	private String endpoint = "/api/notifications";
	private boolean metricsEnabled = false;
}