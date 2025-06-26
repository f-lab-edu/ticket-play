package com.flab.tiple.infrastructure.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.flab.tiple.domain.model.SseConnection;
import com.flab.tiple.domain.repository.NotificationService;
import com.flab.tiple.domain.repository.SseConnectionRepository;
import com.flab.tiple.infrastructure.config.SseProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseNotificationServiceImpl implements NotificationService {
	private final SseConnectionRepository connectionRepository;
	private final SseProperties sseProperties;

	@Override
	public SseEmitter subscribe(Long memberId) {
		SseEmitter emitter = new SseEmitter(sseProperties.getTimeout());
		SseConnection connection = SseConnection.create(memberId, emitter);

		// 이벤트 리스너 등록
		setupEmitterEventListeners(emitter, connection.getConnectionId());

		// 연결 저장
		connectionRepository.save(connection);

		// 연결 확인 메시지 전송
		sendToConnection(connection.getConnectionId(), "SSE 연결 완료", "CONNECT");

		log.info("SSE 연결 생성 - 회원 ID: {}, 연결 ID: {}", memberId, connection.getConnectionId());
		return emitter;
	}

	@Override
	public void sendToMember(Long memberId, Object data, String eventName) {
		List<SseConnection> connections = connectionRepository.findByMemberId(memberId);

		log.info("회원 ID: {}에게 SSE 이벤트 전송 시도 - 이벤트: {}, 연결 수: {}",
			memberId, eventName, connections.size());

		if (connections.isEmpty()) {
			log.warn("SSE_NO_CONNECTION|memberId={}|eventName={}", memberId, eventName);
			return;
		}

		connections.forEach(connection -> {
			sendToConnection(connection.getConnectionId(), data, eventName);
			log.info("SSE_NOTIFICATION_SENT|memberId={}|connectionId={}|eventName={}",
				memberId, connection.getConnectionId(), eventName);
		});
	}

	@Override
	public void sendToConnection(String connectionId, Object data, String eventName) {
		connectionRepository.findById(connectionId)
			.ifPresentOrElse(
				connection -> sendEvent(connection.getEmitter(), connectionId, data, eventName),
				() -> log.warn("연결을 찾을 수 없음 - 연결 ID: {}", connectionId)
			);
	}

	private void sendEvent(SseEmitter emitter, String connectionId, Object data, String eventName) {
		try {
			emitter.send(SseEmitter.event()
				.id(connectionId)
				.name(eventName)
				.data(data));
		} catch (IOException e) {
			log.error("SSE 이벤트 전송 실패 - 연결 ID: {}", connectionId, e);
			connectionRepository.remove(connectionId);
			emitter.completeWithError(e);
		}
	}

	private void setupEmitterEventListeners(SseEmitter emitter, String connectionId) {
		emitter.onCompletion(() -> {
			connectionRepository.remove(connectionId);
			log.info("SSE 연결 완료 - 연결 ID: {}", connectionId);
		});

		emitter.onTimeout(() -> {
			connectionRepository.remove(connectionId);
			log.info("SSE 연결 타임아웃 - 연결 ID: {}", connectionId);
		});

		emitter.onError((e) -> {
			connectionRepository.remove(connectionId);
			log.error("SSE 연결 에러 - 연결 ID: {}", connectionId, e);
		});
	}
}