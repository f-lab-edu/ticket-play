package com.flab.tiple.ticket.reservation.application.event;

import java.time.LocalDateTime;

public interface DomainEvent {
	LocalDateTime getOccurredAt();
}