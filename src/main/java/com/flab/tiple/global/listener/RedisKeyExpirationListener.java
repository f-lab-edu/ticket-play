package com.flab.tiple.global.listener;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.flab.tiple.global.message.TipleRedisKey;
import com.flab.tiple.ticket.reservation.service.TicketReservationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisKeyExpirationListener implements MessageListener {
	private final TicketReservationService ticketReservationService;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String expiredKey = message.toString();

		// 티켓 예약 키인 경우 처리
		if (expiredKey.startsWith(TipleRedisKey.TICKET_RESERVATION_KEY.getKey())) {
			String[] parts = expiredKey.split(":");
			if (parts.length >= 3) {
				Long seatId = Long.parseLong(parts[1]);
				Long memberId = Long.parseLong(parts[2]);
				// 타임아웃 처리 실행
				ticketReservationService.handleReservationTimeout(seatId, memberId);
			}
		}
	}
}