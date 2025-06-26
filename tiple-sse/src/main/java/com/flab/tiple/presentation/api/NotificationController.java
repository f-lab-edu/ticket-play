package com.flab.tiple.presentation.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.flab.tiple.application.SseNotificationUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
	private final SseNotificationUseCase sseNotificationUseCase;

	@GetMapping(value = "/subscribe", produces = "text/event-stream")
	public SseEmitter subscribe(@RequestParam Long memberId) {
		// 실제 환경에서는 JWT 토큰에서 memberId를 추출
		return sseNotificationUseCase.subscribeToNotifications(memberId);
	}

	// 테스트용 엔드포인트 (실제 운영에서는 제거)
	@PostMapping("/send-test")
	public void sendTestNotification(@RequestParam Long memberId, @RequestParam String message) {
		sseNotificationUseCase.sendGeneralNotification(memberId, message, "TEST");
	}
}