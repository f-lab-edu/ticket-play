package com.flab.tiple.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class SseConnection {
	private final String connectionId;
	private final Long memberId;
	private final SseEmitter emitter;
	private final LocalDateTime createdAt;

	public static SseConnection create(Long memberId, SseEmitter emitter) {
		String connectionId = memberId + "_" + System.currentTimeMillis();
		return new SseConnection(connectionId, memberId, emitter, LocalDateTime.now());
	}

	public boolean belongsToMember(Long memberId) {
		return this.memberId.equals(memberId);
	}
}