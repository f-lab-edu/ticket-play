package com.flab.tiple.global.infrastructure.notification.sse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.flab.tiple.global.aop.LoginCheck;
import com.flab.tiple.global.aop.LoginCheckAspect;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "notifications", description = "알림 API")
@Slf4j
public class NotificationController {
	private final SseEmitterService sseEmitterService;

	@GetMapping(value = "/subscribe", produces = "text/event-stream")
	@LoginCheck(required = true)
	public SseEmitter subscribe() {
		Long memberId = LoginCheckAspect.getCurrentMemberId();
		return sseEmitterService.subscribe(memberId);
	}
}