package com.flab.tiple.reservation.infrastructure.timeout;

// TODO batch서버로 이동
/**
 * RedisSortedSetTimeoutHandler 핵심 구조**
 * 1. 타임아웃 스케줄링 시스템
 *	scheduleTimeout(타임 아웃 등록) -> processExpiredRes(스캐줄러 감지) -> TimeoutProcessing(배치 병렬처리)
 *	2. Redis 저장 구조
 *	- reservation_timeouts (ZSet): 타임아웃 대기 중인 예약들
 *	  ─ Score: 타임아웃 시간 (timestamp)
 *	  ─ Value: TicketReservationTimeoutData JSON
 *	  ─ 용도: 시간순 자동 정렬 및 만료 감지
 *	3. 비동기 처리 아키텍처
 *	- @Async + @Scheduled: 스케줄러 비동기 실행
 *	- CompletableFuture: 배치 내 병렬 처리
 *	- ThreadPoolTaskExecutor: 전용 스레드풀 사용
 */

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.reservation.application.event.reservation.ReservationCreatedEvent;
import com.flab.tiple.reservation.domain.model.TicketReservationTimeoutData;
import com.flab.tiple.reservation.domain.service.TimeoutScheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 장애 시나리오별 처리
 *
 * 시나리오 1: 스케줄러 실행 중 Redis 연결 실패
 *  processExpiredReservations() 실행
 *  ↓ rangeByScoreWithScores() Redis 실패
 *  ↓ catch (Exception e) 블록 실행
 *  ↓ log.error("만료 예약 처리 중 오류")
 *  결과: 해당 사이클 건너뛰고 30초 후 재시도
 *
 *  시나리오 2: 배치 처리 중 일부 항목 실패
 *  100개 항목 병렬 처리 시작
 *  - 90개 성공
 *  - 10개 실패
 *  ↓ CompletableFuture.allOf() 완료
 *  ↓ exceptionally() 블록 실행
 *  ↓ log.error("배치 처리 중 오류 발생")
 *  결과: 실패한 10개는 TimeoutProcessingSvc 에서 handleFailedItem() 처리
 *
 * 시나리오 3: JSON 직렬화/역직렬화 오류
 *  scheduleTimeout() 또는 cancelTimeout()
 *  ↓ objectMapper.writeValueAsString() 실패
 *  ↓ catch (Exception e) 블록 실행
 *  ↓ log.error() 기록
 *  결과: 타임아웃 등록/취소 실패, 수동 처리
 *
 * 시나리오 4: 스레드풀 고갈
 *  timeoutTaskExecutor 스레드풀 포화
 *  ↓ CompletableFuture.runAsync() 대기
 *  ↓ ThreadPoolExecutor.CallerRunsPolicy
 *  ↓ 호출한 스레드에서 직접 실행
 *  결과: 성능 저하되지만 처리는 계속됨
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSortedSetTimeoutHandler implements TimeoutScheduler {
	private final RedisTemplate<String, Object> redisTemplate;
	private final ApplicationEventPublisher eventPublisher;
	private final ApplicationContext applicationContext;
	private final ObjectMapper objectMapper;
	private final TimeoutProcessingService timeoutProcessingService;

	private static final String TIMEOUT_ZSET = "reservation_timeouts";


	/**
	 * step 1: 타임아웃 스케줄링
	 * 현재시간 + 10분 계산
	 *  - TicketReservationTimeout
	 *   - Data 객체 생성
	 *   - JSON 직렬화
	 *   - ZSet에 저장
	 *  (score = 타임아웃시간)
	 *
	 *  이를 30초마다 스케줄링
	 * @param event
	 */
	@Override
	public void scheduleTimeout(ReservationCreatedEvent event) {
		try {
			long timeoutTimestamp = System.currentTimeMillis() + (600 * 1000); // 10분 후

			TicketReservationTimeoutData timeoutData = TicketReservationTimeoutData.builder()
				.reservationId(event.getReservationId())
				.seatId(event.getSeatId())
				.memberId(event.getMemberId())
				.concertId(event.getConcertId())
				.build();

			String timeoutValue = objectMapper.writeValueAsString(timeoutData);

			// reservation_timeouts => ZSet형태로 저장 , 대기 중인 타임아웃에 관한 저장소, ️ 시간순 정렬
			redisTemplate.opsForZSet().add(TIMEOUT_ZSET, timeoutValue, timeoutTimestamp);

			log.info("타임아웃 ZSet 저장 완료: reservationId={}, timeoutAt={}",
				event.getReservationId(), timeoutTimestamp);

		} catch (Exception e) {
			log.error("ZSet 저장 실패: reservationId={}", event.getReservationId(), e);
		}
	}

	@Override
	public void cancelTimeout(Long reservationId, Long seatId, Long memberId, Long concertId) {
		try {
			TicketReservationTimeoutData timeoutData = TicketReservationTimeoutData.builder()
				.reservationId(reservationId)
				.seatId(seatId)
				.memberId(memberId)
				.concertId(concertId)
				.build();

			String timeoutValue = objectMapper.writeValueAsString(timeoutData);
			redisTemplate.opsForZSet().remove(TIMEOUT_ZSET, timeoutValue);

			log.info("타임아웃 취소 완료: reservationId={}", reservationId);

		} catch (Exception e) {
			log.error("타임아웃 취소 실패: reservationId={}", reservationId, e);
		}
	}

	/**
	 * step2 : 만료 감지 및 조회
	 * currentTime 기준으로 만료된 항목 조회
	 *  배치 크기: 최대 100개
	 *  rangeByScoreWithScores(0 ~ currentTime)
	 *  만료 항목 리스트 반환
	 */
	// 스케줄러로 만료된 예약 처리 (비동기)
	@Async("timeoutTaskExecutor")
	@Scheduled(fixedRate = 30000) // 30초마다
	public void processExpiredReservations() {
		/**
		 * step3: 비동기 병렬 처리(배치단위)
		 */

		CompletableFuture.runAsync(() -> {
			try {
				long currentTime = System.currentTimeMillis();

				// 배치로 처리할 크기 설정
				int batchSize = 100;
				long startScore = 0;
				long endScore = currentTime;

				// 만료된 항목들을 배치로 조회
				Set<ZSetOperations.TypedTuple<Object>> expiredItems = redisTemplate.opsForZSet()
					.rangeByScoreWithScores(TIMEOUT_ZSET, startScore, endScore, 0, batchSize);

				if (!expiredItems.isEmpty()) {
					log.info("만료된 예약 {}개 처리 시작", expiredItems.size());

					// 병렬 처리
					List<CompletableFuture<Void>> futures = expiredItems.stream()
						.map(item -> CompletableFuture.runAsync(() -> //  // 각 item마다 별도의 CompletableFuture 생성
						{
							try {
								// 실제 처리 로직 (별도 스레드에서 실행됨)
								timeoutProcessingService.processExpiredItem(item);
							} catch (JsonProcessingException e) {
								// 체크드 예외를 런타임 예외로 변환
								throw new RuntimeException(e);
							}
						}, getTimeoutExecutor()))  // 지정된 스레드풀 사용
						.collect(Collectors.toList());

					/**
					 * step4: 모든 작업 완료 대기 및 로깅
					 *  모든 처리 완료 대기
					 *  성공 시: 완료 로그
					 *  실패 시: 에러 로그
					 *
					 *  타임아웃 취소:
					 *   cancelTimeout() 호출 -> JSON 재생성 후 -> ZSet에서 remove
					 */
					CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
						.thenRun(() -> log.info("만료된 예약 배치 처리 완료"))
						.exceptionally(throwable -> {
							log.error("배치 처리 중 오류 발생", throwable);
							return null;
						});
				}

			} catch (Exception e) {
				log.error("만료 예약 처리 중 오류", e);
			}
		}, getTimeoutExecutor());
	}

	private Executor getTimeoutExecutor() {
		return ((ThreadPoolTaskExecutor) applicationContext.getBean("timeoutTaskExecutor")).getThreadPoolExecutor();
	}

}
