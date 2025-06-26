package com.flab.tiple.ticket.reservation.application.event.handler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.flab.tiple.global.message.TipleMessage;
import com.flab.tiple.global.message.TipleRedisKey;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationApprovedEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationCanceledEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationCreatedEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationNotificationEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationTimeoutEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.SeatAvailableNotificationEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.WaitingNumberNotificationEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.WaitingRegisteredEvent;
import com.flab.tiple.ticket.reservation.application.notification.ReservationNotificationService;
import com.flab.tiple.ticket.reservation.domain.model.TicketReservationRedis;
import com.flab.tiple.ticket.reservation.domain.model.enums.TicketReservationStatus;
import com.flab.tiple.ticket.reservation.domain.repository.TicketReservationRedisRepository;
import com.flab.tiple.ticket.reservation.domain.service.TimeoutScheduler;
import com.flab.tiple.ticket.waiting.domain.TicketWaitingRedis;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * 핵심 구조
 * 예약 이벤트 수신 처리 -> 비즈니스 로직 실행 -> 알림 이벤트 실행(때에 따라)
 *
 * 1. 처리 이벤트 타입
 * - ReservationCreatedEvent: 예약 생성 → Redis 저장 + 타임아웃 스케줄링
 * - ReservationTimeoutEvent: 예약 타임아웃 → 다음 대기자 처리
 * - ReservationApprovedEvent: 예약 승인 → Redis 정리 + 타임아웃 취소 + 알림
 * - ReservationCanceledEvent: 예약 취소 → 타임아웃 취소 + 다음 대기자 처리
 * - WaitingRegisteredEvent: 대기 등록 → 대기번호 알림
 *
 * 2. 비동기 처리 아키텍처
 * - @Async + CompletableFuture: 작업별 병렬 실행
 * - ApplicationEventPublisher: 느슨한 결합의 이벤트 발행
 * - 의존성 분리: TimeoutScheduler 인터페이스 사용
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventHandler {
	private final TicketWaitingRedisRepository ticketWaitingRedisRepository;
	private final TicketReservationRedisRepository ticketReservationRedisRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final ReservationNotificationService notificationService;
	private final int REDIS_KEY_TTL = 600;
	private final ApplicationEventPublisher eventPublisher;
	private final TimeoutScheduler timeoutScheduler; // 인터페이스 의존

	/**
	 * @Async("eventTaskExecutor")
	 * 1. "eventTaskExecutor"라는 이름의 TaskExecutor 빈을 찾아서 사용
	 * 2. 해당 스레드풀에서 메소드를 비동기로 실행
	 * 3. 만약 해당 이름의 빈이 없으면 기본 TaskExecutor 사용
	 */
	@EventListener
	@Async("eventTaskExecutor")
	public void handleReservationCreated(ReservationCreatedEvent event) {
		log.info("예약 생성 이벤트 처리: reservationId={}, memberId={}",
			event.getReservationId(), event.getMemberId());

		CompletableFuture.allOf(
			// 예약 정보 Redis 저장
			CompletableFuture.runAsync(() -> saveReservationToRedis(event.getSeatId(), event.getMemberId(), event.getConcertId())),
			// 타임아웃 스케줄링
			CompletableFuture.runAsync(() -> timeoutScheduler.scheduleTimeout(event))
		).thenRun(() -> {
			log.info("예약 생성 처리 완료: reservationId={}", event.getReservationId());
		}).exceptionally(throwable -> {
			log.error("예약 생성 처리 실패: reservationId={}", event.getReservationId(), throwable);
			return null;
		});
	}

	@EventListener
	@Async("eventTaskExecutor")
	public void handleReservationCanceled(ReservationCanceledEvent event) {
		log.info("예약 취소 이벤트 처리: reservationId={}", event.getReservationId());

		CompletableFuture.allOf(
			// 타임아웃 취소
			CompletableFuture.runAsync(() -> timeoutScheduler.cancelTimeout(
				event.getReservationId(), event.getSeatId(),
				event.getMemberId(), event.getConcertId())),
			// 다음 대기자 처리
			CompletableFuture.runAsync(() -> processNextWaitingUser(
				event.getConcertId(), event.getSeatId()))
		).exceptionally(throwable -> {
			log.error("예약 취소 처리 실패: reservationId={}", event.getReservationId(), throwable);
			return null;
		});
	}

	@EventListener
	public void handleReservationTimeout(ReservationTimeoutEvent event) {
		log.info("예약 타임아웃 이벤트 처리: reservationId={}", event.getReservationId());

		// 타임아웃 된 좌석에 대해 다음 대기자 알림 처리
		processNextWaitingUser(event.getConcertId(), event.getSeatId());
	}

	@EventListener
	@Async("eventTaskExecutor")
	public void handleReservationApproved(ReservationApprovedEvent event) {
		log.info("예약 승인 이벤트 처리: reservationId={}", event.getReservationId());

		CompletableFuture.allOf(
			// Redis 정보 삭제
			CompletableFuture.runAsync(() -> cleanupRedisData(event)),
			// 타임아웃 취소
			CompletableFuture.runAsync(() -> timeoutScheduler.cancelTimeout(
				event.getReservationId(), event.getSeatId(),
				event.getMemberId(), event.getConcertId())),
			// 알림 이벤트 발행
			CompletableFuture.runAsync(() -> publishNotificationEvent(event))
		).thenRun(() -> {
			log.info("예약 승인 처리 완료: reservationId={}", event.getReservationId());
		}).exceptionally(throwable -> {
			log.error("예약 승인 처리 실패: reservationId={}", event.getReservationId(), throwable);
			return null;
		});
	}

	@EventListener
	public void handleWaitingRegistered(WaitingRegisteredEvent event) {
		log.info("대기 등록 이벤트 처리: concertId={}, memberId={}, waitingNumber={}",
			event.getConcertId(), event.getMemberId(), event.getWaitingNumber());
		// 변경: 직접 호출 대신 이벤트 발행
		WaitingNumberNotificationEvent notificationEvent = WaitingNumberNotificationEvent.builder()
			.memberId(event.getMemberId())
			.concertId(event.getConcertId())
			.waitingNumber(event.getWaitingNumber())
			.message("현재 대기번호는 " + event.getWaitingNumber() + "번입니다.")
			.build();

		eventPublisher.publishEvent(notificationEvent);
		log.info("대기번호 알림 이벤트 발행: memberId={}, waitingNumber={}",
			event.getMemberId(), event.getWaitingNumber());
	}

	// 다음 대기자 알림 처리 메서드
	private void processNextWaitingUser(Long concertId, Long seatId) {
		try {
			// 대기자 목록 조회
			List<TicketWaitingRedis> waitingList =
				ticketWaitingRedisRepository.findByConcertIdOrderByWaitingNumberAsc(concertId);

			if (waitingList.isEmpty()) {
				return;
			}

			// 첫 번째 대기자에게 알림
			TicketWaitingRedis nextWaiting = waitingList.get(0);

			// 변경: 직접 호출 대신 이벤트 발행
			SeatAvailableNotificationEvent notificationEvent = SeatAvailableNotificationEvent.builder()
				.memberId(nextWaiting.getMemberId())
				.seatId(seatId)
				.concertId(concertId)
				.message(TipleMessage.RESERVATION_CONCERT_AVAILE_MESSAGE.getMessage())
				.build();

			eventPublisher.publishEvent(notificationEvent);
		} catch (Exception e) {
			log.error("대기자 처리 중 예외 발생: {}", e.getMessage(), e);
		}
	}

	private void cleanupRedisData(ReservationApprovedEvent event) {
		String redisKey = event.getSeatId() + ":" + event.getMemberId();
		ticketReservationRedisRepository.deleteById(redisKey);
		log.info("Redis 예약 정보 삭제 완료: {}", redisKey);
	}

	private void publishNotificationEvent(ReservationApprovedEvent event) {
		ReservationNotificationEvent notificationEvent = ReservationNotificationEvent.builder()
			.reservationId(event.getReservationId())
			.seatId(event.getSeatId())
			.memberId(event.getMemberId())
			.concertId(event.getConcertId())
			.status(TicketReservationStatus.APPROVED)
			.message("예약이 승인되었습니다.")
			.build();

		eventPublisher.publishEvent(notificationEvent);
	}
	// Redis에 예약 정보 저장 (10분 타이머)
	private void saveReservationToRedis(Long seatId, Long memberId, Long concertId) {
		TicketReservationRedis reservationRedis = TicketReservationRedis.builder()
			.seatId(seatId)
			.memberId(memberId)
			.concertId(concertId)
			.build();

		ticketReservationRedisRepository.save(reservationRedis);

		// 만료 시 자동 처리를 위한 리스너 설정
		redisTemplate.expire(
			TipleRedisKey.TICKET_RESERVATION_KEY.getKey() + reservationRedis.getId(),
			REDIS_KEY_TTL,
			TimeUnit.SECONDS
		);

		log.info("Redis에 예약 타이머 설정 완료: seatId={}, memberId={}, ttl={}초",
			seatId, memberId, REDIS_KEY_TTL);
	}
}