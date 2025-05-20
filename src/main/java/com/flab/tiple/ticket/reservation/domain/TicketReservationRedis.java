package com.flab.tiple.ticket.reservation.domain;


import java.time.LocalDateTime;

import org.springframework.data.redis.core.RedisHash;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;

/**
 * 이 어노테이션은 Spring Data Redis에서 사용하는 것으로, Redis에 객체를 해시 구조로 저장할 때 사용합니다.
 * 의미:
 * value = "ticketReservation"
 * → Redis에서 사용할 키 prefix입니다.
 * → 실제 Redis 키 형식은: ticketReservation:{id}
 *
 * timeToLive = 600
 * → 해당 객체는 Redis에 저장된 후 10분(600초) 후 만료됩니다.
 * → TTL(Time To Live)을 설정하는 것입니다.
 */
@RedisHash(value = "ticketReservation", timeToLive = 600) // 10분 TTL
@Getter
public class TicketReservationRedis {
	@Id
	private String id;
	private Long seatId;
	private Long memberId;
	private Long concertId;
	private LocalDateTime reservedAt;

	@Builder
	public TicketReservationRedis(Long seatId, Long memberId, Long concertId) {
		this.id = seatId + ":" + memberId;
		this.seatId = seatId;
		this.memberId = memberId;
		this.concertId = concertId;
		this.reservedAt = LocalDateTime.now();
	}
}