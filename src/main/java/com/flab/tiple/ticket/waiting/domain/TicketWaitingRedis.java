package com.flab.tiple.ticket.waiting.domain;

import java.time.LocalDateTime;

import org.springframework.data.redis.core.RedisHash;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;

@RedisHash(value = "ticketWaiting", timeToLive = 86400) // 24시간 TTL
@Getter
public class TicketWaitingRedis {
	@Id
	private String id;
	private Long concertId;
	private Long memberId;
	private Integer waitingNumber;
	private String status;
	private LocalDateTime createdAt;

	@Builder
	public TicketWaitingRedis(Long concertId, Long memberId, Integer waitingNumber,
		String status) {
		this.id = concertId + ":" + memberId;
		this.concertId = concertId;
		this.memberId = memberId;
		this.waitingNumber = waitingNumber;
		this.status = status;
		this.createdAt = LocalDateTime.now();
	}

	public void changeStatus(String status) {
		this.status = status;
	}
}