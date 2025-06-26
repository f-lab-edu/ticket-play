package com.flab.tiple.reservation.application.event.reservation;


import com.flab.tiple.reservation.application.event.AbstractDomainEvent;
import com.flab.tiple.waiting.domain.model.TicketWaitingRedis;

import lombok.Builder;
import lombok.Getter;

@Getter
public class WaitingRegisteredEvent extends AbstractDomainEvent {
	private final Long concertId;
	private final Long memberId;
	private final int waitingNumber;

	@Builder
	public WaitingRegisteredEvent(Long concertId, Long memberId, int waitingNumber) {
		super();
		this.concertId = concertId;
		this.memberId = memberId;
		this.waitingNumber = waitingNumber;
	}

	public static WaitingRegisteredEvent from(TicketWaitingRedis waiting) {
		return WaitingRegisteredEvent.builder()
			.concertId(waiting.getConcertId())
			.memberId(waiting.getMemberId())
			.waitingNumber(waiting.getWaitingNumber())
			.build();
	}
}
