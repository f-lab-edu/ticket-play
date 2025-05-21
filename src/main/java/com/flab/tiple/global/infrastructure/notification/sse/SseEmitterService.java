package com.flab.tiple.global.infrastructure.notification.sse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *  SSE(Server-Sent Events)는 브라우저 또는 클라이언트가 서버와 HTTP 연결을 유지한 채로 서버로부터 실시간 이벤트를 push로 전달받는 방식.
 * 여기서는 클라이언트가 1번 구독 요청을 보내면, 서버가 그 요청에 대해 **HTTP 응답을 ‘계속 열어둔 상태’**로 만들어서 이벤트를 지속적으로 보낼 수 있게 되는 동작 메커니즘을 가지고 있음.
 *
 * ✅ SseEmitter 동작 메커니즘: 어떻게 연결을 유지하며 메시지를 받는가?
 *
 *	1. 클라이언트가 구독 요청 (subscribe)
 *	GET /sse/subscribe
 *	Accept: text/event-stream
 *	클라이언트는 text/event-stream이라는 Accept 헤더를 넣고 서버에 요청을 보냄.
 *	이걸 처리하는 서버측 메서드에서 SseEmitter 객체를 만들어 반환.
 *
 *	2. 서버는 응답 스트림을 열어둔 채 유지
 *	SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT); // 60분 유지
 *	return emitter;
 *	이 emitter는 Spring MVC에서 반환되면, Spring은 HTTP 응답을 끝내지 않고 스트림을 열어둠.
 *	서버는 응답을 "끝내지 않고", 계속 이벤트를 보낼 수 있음.
 *
 *	3. 서버는 emitter.send(...)를 호출해서 실시간 데이터 전송
 *		emitter.send(SseEmitter.event().id(...).name(...).data(...));
 *	이 호출은 클라이언트가 아직 HTTP 연결을 유지하고 있다는 전제 하에, 클라이언트로 event 메시지를 보냄.
 *
 * 	4. 연결이 끊어지면 (timeout, error, completion) emitter가 제거됨
 * 	emitter.onTimeout(() -> emitters.remove(emitterId));
 * 	emitter.onCompletion(() -> emitters.remove(emitterId));
 * 	emitter.onError(e -> emitters.remove(emitterId));
 * 	클라이언트가 브라우저를 끄거나, 네트워크가 끊기거나, 설정된 timeout이 지나면 Spring이 onTimeout, onCompletion 등을 호출.
 * 	이때 연결된 emitter를 메모리에서 정리.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseEmitterService {
	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
	private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 60분

	public SseEmitter subscribe(Long memberId) {
		String emitterId = makeTimeIncludeId(memberId);
		SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

		emitter.onCompletion(() -> {
			emitters.remove(emitterId);
		});
		emitter.onTimeout(() -> {
			emitters.remove(emitterId);
		});
		emitter.onError((e) -> {
			emitters.remove(emitterId);
		});

		// 더미 이벤트 전송
		sendToClient(emitter, emitterId, "SSE 연결 완료", "CONNECT");

		emitters.put(emitterId, emitter);

		return emitter;
	}

	public void sendToClient(SseEmitter emitter, String emitterId, Object data, String eventName) {
		try {
			emitter.send(SseEmitter.event()
				.id(emitterId)
				.name(eventName)
				.data(data));

		} catch (IOException exception) {
			emitters.remove(emitterId);
			emitter.completeWithError(exception);
		}
	}

	public void sendToMember(Long memberId, Object data, String eventName) {
		// memberId로 모든 연결 찾기 (접두어로 비교)
		String prefix = memberId + "_";

		log.info("회원 ID: {}에게 SSE 이벤트 전송 시도 - 이벤트: {}", memberId, eventName);

		boolean sent = false;
		for (String key : emitters.keySet()) {
			if (key.startsWith(prefix)) {
				sendToClient(emitters.get(key), key, data, eventName);
				sent = true;
				log.info("SSE_NOTIFICATION_SENT|memberId={}|eventName={}|data={}",
					memberId, eventName, data);
			}
		}

		if (!sent) {
			log.warn("SSE_NO_CONNECTION|memberId={}|eventName={}", memberId, eventName);
		}
	}

	private String makeTimeIncludeId(Long memberId) {
		return memberId + "_" + System.currentTimeMillis();
	}
}