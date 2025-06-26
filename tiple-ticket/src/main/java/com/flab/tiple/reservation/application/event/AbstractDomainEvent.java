package com.flab.tiple.reservation.application.event;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public abstract class AbstractDomainEvent implements DomainEvent {
	private final LocalDateTime occurredAt;

	protected AbstractDomainEvent() {
		this.occurredAt = LocalDateTime.now();
	}
}