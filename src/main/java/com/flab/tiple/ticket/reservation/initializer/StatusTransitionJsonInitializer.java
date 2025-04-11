package com.flab.tiple.ticket.reservation.initializer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.ticket.reservation.domain.TicketReservation;
import com.flab.tiple.ticket.reservation.enums.TicketReservationStatus;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StatusTransitionJsonInitializer {

	private static final String JSON_FILE_PATH = "json/status-transitions.json";
	private final ObjectMapper objectMapper;

	public StatusTransitionJsonInitializer() {
		this.objectMapper = new ObjectMapper();
	}

	@PostConstruct
	public void initialize() {
		try {
			// JSON 파일에서 설정 로드
			Map<TicketReservationStatus, Set<TicketReservationStatus>> transitionsMap = loadTransitionsFromJson();

			// 엔티티 클래스의 정적 맵 초기화
			TicketReservation.initializeStatusTransitions(transitionsMap);
			log.info("Successfully initialized status transitions from JSON");

		} catch (Exception e) {
			// 설정을 로드할 수 없는 경우 기본값 사용
			Map<TicketReservationStatus, Set<TicketReservationStatus>> defaultMap = createDefaultTransitionsMap();
			TicketReservation.initializeStatusTransitions(defaultMap);

			log.warn("Failed to load status transitions from JSON. Using default values.", e);
		}
	}

	private Map<TicketReservationStatus, Set<TicketReservationStatus>> loadTransitionsFromJson() throws IOException {
		InputStream inputStream = loadJsonFileFromClasspath();
		JsonNode transitionsNode = readTransitionsFromJson(inputStream);
		return convertJsonToTransitionMap(transitionsNode);
	}

	/**
	 * 1. 클래스패스에서 JSON 파일 로드
	 */
	private InputStream loadJsonFileFromClasspath() throws FileNotFoundException {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(JSON_FILE_PATH);
		if (inputStream == null) {
			throw new FileNotFoundException("Cannot find " + JSON_FILE_PATH + " in classpath");
		}
		return inputStream;
	}

	/**
	 * 2. JSON 파일 읽기
	 */
	private JsonNode readTransitionsFromJson(InputStream inputStream) throws IOException {
		JsonNode rootNode = objectMapper.readTree(inputStream);
		JsonNode transitionsNode = rootNode.get("statusTransitions");

		if (transitionsNode == null || !transitionsNode.isObject()) {
			throw new IllegalStateException("Invalid JSON format: missing or invalid 'statusTransitions' object");
		}

		return transitionsNode;
	}

	/**
	 * 3. JSON 데이터를 Map으로 변환
	 */
	private Map<TicketReservationStatus, Set<TicketReservationStatus>> convertJsonToTransitionMap(JsonNode transitionsNode) {
		Map<TicketReservationStatus, Set<TicketReservationStatus>> result = new HashMap<>();

		Iterator<Map.Entry<String, JsonNode>> fields = transitionsNode.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			String targetStatusName = entry.getKey();
			TicketReservationStatus targetStatus = TicketReservationStatus.valueOf(targetStatusName);

			Set<TicketReservationStatus> sourceStatuses = new HashSet<>();
			JsonNode sourceStatusesNode = entry.getValue();

			if (sourceStatusesNode.isArray()) {
				for (JsonNode node : sourceStatusesNode) {
					if (node.isTextual()) {
						sourceStatuses.add(TicketReservationStatus.valueOf(node.asText()));
					}
				}
			}

			result.put(targetStatus, sourceStatuses);
		}

		return result;
	}

	public static Map<TicketReservationStatus, Set<TicketReservationStatus>> createDefaultTransitionsMap() {
		// 기본 상태 전이 규칙 생성
		Map<TicketReservationStatus, Set<TicketReservationStatus>> defaultMap = new HashMap<>();
		defaultMap.put(TicketReservationStatus.APPROVED, Set.of(TicketReservationStatus.PENDING));
		defaultMap.put(TicketReservationStatus.CANCELLED,
			Set.of(TicketReservationStatus.PENDING, TicketReservationStatus.APPROVED));
		return defaultMap;
	}

	// 테스트에서 사용할 수 있는 초기화 메서드
	public static void initializeForTest() {
		TicketReservation.initializeStatusTransitions(createDefaultTransitionsMap());
	}
}