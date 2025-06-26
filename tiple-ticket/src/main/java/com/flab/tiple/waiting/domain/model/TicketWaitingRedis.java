package com.flab.tiple.waiting.domain.model;

import java.time.LocalDateTime;

import org.springframework.data.redis.core.RedisHash;

import com.flab.tiple.exception.ErrorCode;
import com.flab.tiple.waiting.domain.exception.TicketWaitingNotCancelableException;

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
	private TicketWaitingStatus status;
	private LocalDateTime createdAt;

	@Builder
	public TicketWaitingRedis(Long concertId, Long memberId, Integer waitingNumber,
		TicketWaitingStatus status) {
		this.id = concertId + ":" + memberId;
		this.concertId = concertId;
		this.memberId = memberId;
		this.waitingNumber = waitingNumber;
		this.status = status;
		this.createdAt = LocalDateTime.now();
	}

	public static void validateCancel(TicketWaitingRedis ticketWaiting){
		if (ticketWaiting.getStatus().equals(TicketWaitingStatus.WAITING)) {
			throw new TicketWaitingNotCancelableException(
				ErrorCode.TICKET_WAITING_NOT_CANCELABLE,
				ErrorCode.TICKET_WAITING_NOT_CANCELABLE.getDescription()
			);
		}
	}

	public void changeStatus(TicketWaitingStatus status) {
		this.status = status;
	}
}