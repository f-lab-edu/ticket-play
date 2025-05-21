package com.flab.tiple.ticket.reservation.application.event.handler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.flab.tiple.global.message.TipleMessage;
import com.flab.tiple.global.message.TipleRedisKey;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationApprovedEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationCanceledEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationCreatedEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationTimeoutEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.WaitingRegisteredEvent;
import com.flab.tiple.ticket.reservation.application.notification.ReservationNotificationService;
import com.flab.tiple.ticket.reservation.domain.model.TicketReservationRedis;
import com.flab.tiple.ticket.reservation.domain.model.enums.TicketReservationStatus;
import com.flab.tiple.ticket.reservation.domain.repository.TicketReservationRedisRepository;
import com.flab.tiple.ticket.waiting.domain.TicketWaitingRedis;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventHandler {
	private final TicketWaitingRedisRepository ticketWaitingRedisRepository;
	private final TicketReservationRedisRepository ticketReservationRedisRepository;
	private final RedisTemplate<String, Object> redisTemplate;
	private final ReservationNotificationService notificationService;
	private final int REDIS_KEY_TTL = 600;

	@EventListener
	public void handleReservationCreated(ReservationCreatedEvent event) {
		log.info("예약 생성 이벤트 처리: reservationId={}, memberId={}", event.getReservationId(), event.getMemberId());

		// 예약 생성 시 Redis에 타이머 설정
		saveReservationToRedis(event.getSeatId(), event.getMemberId(), event.getConcertId());
	}

	@EventListener
	public void handleReservationCanceled(ReservationCanceledEvent event) {
		log.info("예약 취소 이벤트 처리: reservationId={}", event.getReservationId());

		// 취소된 좌석에 대해 다음 대기자 알림 처리
		processNextWaitingUser(event.getConcertId(), event.getSeatId());
	}

	@EventListener
	public void handleReservationTimeout(ReservationTimeoutEvent event) {
		log.info("예약 타임아웃 이벤트 처리: reservationId={}", event.getReservationId());

		// 타임아웃 된 좌석에 대해 다음 대기자 알림 처리
		processNextWaitingUser(event.getConcertId(), event.getSeatId());
	}

	@EventListener
	public void handleReservationApproved(ReservationApprovedEvent event) {
		log.info("예약 승인 이벤트 처리: reservationId={}", event.getReservationId());

		// Redis에서 예약 타이머 정보 삭제
		String redisKey = event.getSeatId() + ":" + event.getMemberId();
		ticketReservationRedisRepository.deleteById(redisKey);

		// 예약자에게 상태 변경 알림
		notificationService.notifyReservationStatusChanged(
			event.getMemberId(),
			event.getReservationId(),
			TicketReservationStatus.APPROVED
		);
	}

	@EventListener
	public void handleWaitingRegistered(WaitingRegisteredEvent event) {
		log.info("대기 등록 이벤트 처리: concertId={}, memberId={}, waitingNumber={}",
			event.getConcertId(), event.getMemberId(), event.getWaitingNumber());

		// 대기 번호 알림
		notificationService.notifyWaitingNumber(
			event.getMemberId(),
			event.getConcertId(),
			event.getWaitingNumber()
		);
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

			// 알림 서비스를 통해 알림 전송
			notificationService.notifySeatAvailable(
				nextWaiting.getMemberId(),
				seatId,
				concertId,
				TipleMessage.RESERVATION_CONCERT_AVAILE_MESSAGE.getMessage()
			);

			log.info("대기자 알림 전송 완료: memberId={}, concertId={}, seatId={}",
				nextWaiting.getMemberId(), concertId, seatId);
		} catch (Exception e) {
			log.error("대기자 처리 중 예외 발생: {}", e.getMessage(), e);
		}
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