package com.flab.tiple.domain.repository;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {
	SseEmitter subscribe(Long memberId);
	void sendToMember(Long memberId, Object data, String eventName);
	void sendToConnection(String connectionId, Object data, String eventName);
}