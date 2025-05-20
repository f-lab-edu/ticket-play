package com.flab.tiple.global.message;

import lombok.Getter;

@Getter
public enum TipleRedisKey {
	TICKET_RESERVATION_KEY("ticketReservation:"),
	TICKET_WAITING_COUNTER_KEY("concert:waiting:counter:");
	private final String key;

	TipleRedisKey(String key) {
		this.key = key;
	}
}
