package com.flab.tiple.reservation.application.event;

import java.time.LocalDateTime;

public interface DomainEvent {
	LocalDateTime getOccurredAt();
}