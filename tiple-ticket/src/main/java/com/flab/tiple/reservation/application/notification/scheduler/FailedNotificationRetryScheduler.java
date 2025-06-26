package com.flab.tiple.reservation.application.notification.scheduler;

import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.reservation.application.dto.response.FailedNotificationDto;
import com.flab.tiple.reservation.application.event.reservation.ReservationNotificationEvent;
import com.flab.tiple.reservation.application.event.reservation.SeatAvailableNotificationEvent;
import com.flab.tiple.reservation.application.event.reservation.WaitingNumberNotificationEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// TODO batch서버 옮기기

/**
 * 핵심 구조
 *
 * 1. 실패 알림 재시도 시스템
 *  실패 감지 및 재시도 스케줄 -> 재시도 처리 -> DLQ 최종이동 또는 성공
 *
 * 2. Redis 저장 구조
 * - failed_notifications (ZSet): 재시도 대기 중인 실패 알림
 *   ─ Score: 다음 재시도 시간 (timestamp)
 *   ─ Value: FailedNotificationDto JSON
 *   ─ 용도: 재시도 시간 기반 자동 정렬
 *
 * - notification_dlq (List): 최종 실패한 알림
 *   ─ 크기 제한: 최대 1000개
 *   ─ 데이터: 최종 실패 정보
 *   ─ 용도: 관리자 수동 처리
 *
 * 3. 재시도 전략
 * - 최대 재시도: 5회
 * - 스케줄 주기: 2분마다
 */

/**
 * 장애 시나리오별 처리
 *
 * 시나리오 1: 재시도 중 이벤트 발행 실패
 *  retryNotification() 실행
 *  ↓ eventPublisher.publishEvent() 실패
 *  ↓ catch (Exception e) 블록 실행
 *  ↓ updateRetryData() 호출
 *  ├─ attemptCount 증가 (1→2)
 *  ├─ 다음 재시도: 2^2 * 5 = 20분 후
 *  └─ ZSet에 업데이트된 정보로 재등록
 *  결과: 재시도 예약됨
 *
 * 시나리오 2: 최대 재시도 횟수 초과
 *  processRetryItem() 실행
 *  ↓ attemptCount >= 5 확인
 *  ↓ moveToDeadLetterQueue() 호출
 *   ─ notification_dlq List에 저장
 *   ─ DLQ 크기 제한 (1000개)
 *   ─ failed_notifications에서 제거
 *   ─ 관리자 수동 처리 대기
 *  결과: 자동 재시도 포기, 수동 처리 필요
 *
 *시나리오 3: JSON 파싱 오류
 *  processRetryItem() 실행
 *  ↓ objectMapper.readValue() 실패
 *  ↓ JsonProcessingException 발생
 *  ↓ catch 블록에서 로그 기록
 *  ↓ 해당 항목 건너뛰고 다음 항목 처리
 *  결과: 손상된 데이터는 ZSet에 남아있음 수동으로 cleanupOldFailed... 에서 일주일 후 자동 정리됨
 *
 * 시나리오 4: Redis 연결 실패
 *  retryFailedNotifications() 실행
 *  ↓ rangeByScoreWithScores() Redis 실패
 *  ↓ catch (Exception e) 블록 실행
 *  ↓ log.error("재시도 스케줄러 오류")
 *  결과: 해당 사이클 건너뛰고 2분 후 재시도
 *        Redis 복구 시 정상 처리 재개
 *
 * 시나리오 5: DLQ 저장 실패
 *  moveToDeadLetterQueue() 실행
 *  ↓ leftPush() 또는 trim() Redis 실패
 *  ↓ catch (Exception e) 블록 실행
 *  ↓ log.error("DLQ 이동 실패")
 *  결과: 실패 정보 손실, 로그에만 기록 failed_notifications에 남아있어 다음 사이클에 재시도 계속됨
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FailedNotificationRetryScheduler {
	private final RedisTemplate<String, Object> redisTemplate;
	private final ApplicationEventPublisher eventPublisher;
	private final ObjectMapper objectMapper;

	private final String FAILED_NOTIFICATION_ZSET = "failed_notifications";
	private final String NOTIFICATION_DLQ = "notification_dlq";
	private final int MAX_RETRY_COUNT = 5;
	private final int SCHEDULE_RATE = 120000;

	/**
	 * step 1: 재시도 대상 감지
	 *  ─ currentTime 기준
	 *  ─ 재시도 시간이 된 항목
	 *    rangeByScoreWithScores
	 *    (0 ~ currentTime)
	 *  ─ 최대 50개씩 배치
	 *  ─ 재시도 대상 리스트
	 */
	// 2분마다 실패한 알림 재시도
	@Scheduled(fixedRate = SCHEDULE_RATE) // 2분마다
	@Async("notificationTaskExecutor")
	public void retryFailedNotifications() {
		try {
			long currentTime = System.currentTimeMillis();

			// 재시도 시간이 된 항목들 조회
			Set<ZSetOperations.TypedTuple<Object>> retryItems = redisTemplate.opsForZSet()
				.rangeByScoreWithScores(FAILED_NOTIFICATION_ZSET, 0, currentTime, 0, 50); // 최대 50개씩

			if (retryItems.isEmpty()) {
				return;
			}

			log.info("실패한 알림 재시도 시작: {}개", retryItems.size());

			for (ZSetOperations.TypedTuple<Object> item : retryItems) {
				try {
					processRetryItem(item);
				} catch (Exception e) {
					log.error("재시도 항목 처리 실패: {}", item.getValue(), e);
				}
			}

		} catch (Exception e) {
			log.error("실패한 알림 재시도 스케줄러 오류", e);
		}
	}

	/**
	 * step 2: 재시도 횟수 검증
	 * ─ JSON → FailedNotificationDto 변환
	 * ─ attemptCount 확인
	 *  ─ if attemptCount >= 5:
	 *     ─ moveToDeadLetter ->Queue() 호출 ->ZSet에서 제거
	 */
	private void processRetryItem(ZSetOperations.TypedTuple<Object> item) throws JsonProcessingException {
		String failedDataJson = (String) item.getValue();
		FailedNotificationDto failedData = objectMapper.readValue(failedDataJson, FailedNotificationDto.class);

		// 최대 재시도 횟수 초과 시 DLQ로 이동
		if (failedData.getAttemptCount() >= MAX_RETRY_COUNT) {
			moveToDeadLetterQueue(failedData);
			redisTemplate.opsForZSet().remove(FAILED_NOTIFICATION_ZSET, failedDataJson);
			return;
		}

		try {
			// 재시도 실행
			retryNotification(failedData);
			// 성공 시 제거
			redisTemplate.opsForZSet().remove(FAILED_NOTIFICATION_ZSET, failedDataJson);
			log.info("실패한 알림 재시도 성공: id={}, type={}, memberId={}",
				failedData.getId(), failedData.getType(), failedData.getMemberId());
		} catch (Exception e) {
			// 실패 시 재시도 횟수 증가 및 다음 재시도 시간 설정
			updateRetryData(failedData, failedDataJson, e.getMessage());
			log.warn("실패한 알림 재시도 실패: id={}, attemptCount={}, error={}",
				failedData.getId(), failedData.getAttemptCount() + 1, e.getMessage());
		}
	}

	/**
	 * step 3: 알림 재시도 실행
	 * @param failedData
	 * @throws JsonProcessingException
	 */
	private void retryNotification(FailedNotificationDto failedData) throws JsonProcessingException {
		String eventJson = failedData.getOriginalEventData();

		switch (failedData.getType()) {
			case "reservation":
				ReservationNotificationEvent reservationEvent =
					objectMapper.readValue(eventJson, ReservationNotificationEvent.class);
				eventPublisher.publishEvent(reservationEvent);
				break;

			case "waiting":
				WaitingNumberNotificationEvent waitingEvent =
					objectMapper.readValue(eventJson, WaitingNumberNotificationEvent.class);
				eventPublisher.publishEvent(waitingEvent);
				break;

			case "seat":
				SeatAvailableNotificationEvent seatEvent =
					objectMapper.readValue(eventJson, SeatAvailableNotificationEvent.class);
				eventPublisher.publishEvent(seatEvent);
				break;

			default:
				throw new IllegalArgumentException("알 수 없는 알림 타입: " + failedData.getType());
		}
	}

	private void updateRetryData(FailedNotificationDto failedData, String originalJson, String errorMessage) {
		try {
			// 재시도 횟수 증가
			failedData.increaseAttemptCount();
			failedData.changeRetryStatus();

			// 지수 백오프: 2^attemptCount * 5분
			long backoffMinutes = (long) Math.pow(2, failedData.getAttemptCount()) * 5;
			long nextRetryTime = System.currentTimeMillis() + (backoffMinutes * 60 * 1000);
			failedData.changeNextRetryAt(nextRetryTime);

			String updatedJson = objectMapper.writeValueAsString(failedData);

			// 기존 항목 제거 후 새로운 시간으로 추가
			redisTemplate.opsForZSet().remove(FAILED_NOTIFICATION_ZSET, originalJson);
			redisTemplate.opsForZSet().add(FAILED_NOTIFICATION_ZSET, updatedJson, nextRetryTime);

		} catch (Exception e) {
			log.error("재시도 데이터 업데이트 실패", e);
		}
	}

	private void moveToDeadLetterQueue(FailedNotificationDto failedData) {
		try {
			Map<String, Object> dlqData = Map.of(
				"id", failedData.getId(),
				"type", failedData.getType(),
				"memberId", failedData.getMemberId(),
				"originalEventData", failedData.getOriginalEventData(),
				"totalAttempts", failedData.getAttemptCount(),
				"timestamp", System.currentTimeMillis()
			);

			String dlqDataJson = objectMapper.writeValueAsString(dlqData);
			redisTemplate.opsForList().leftPush(NOTIFICATION_DLQ, dlqDataJson);

			// DLQ 크기 제한
			redisTemplate.opsForList().trim(NOTIFICATION_DLQ, 0, 999);

		} catch (Exception e) {
			log.error("DLQ 이동 실패", e);
		}
	}

	// 매일 새벽 3시에 오래된 실패 데이터 정리
	@Scheduled(cron = "0 0 3 * * ?")
	public void cleanupOldFailedNotifications() {
		try {
			long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
			Long removedCount = redisTemplate.opsForZSet()
				.removeRangeByScore(FAILED_NOTIFICATION_ZSET, 0, oneWeekAgo);

			log.info("오래된 실패한 알림 정리 완료: {}개", removedCount);

		} catch (Exception e) {
			log.error("실패한 알림 정리 중 오류", e);
		}
	}
}