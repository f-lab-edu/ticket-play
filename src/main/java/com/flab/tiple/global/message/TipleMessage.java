package com.flab.tiple.global.message;

import lombok.Getter;

@Getter
public enum TipleMessage {
	RESERVATION_CONCERT_AVAILE_MESSAGE("예매하신 콘서트 좌석이 현재 가능해졌습니다. 10분 내에 예매를 완료해주세요."),
	RESERVATION_SSE_EVENT_NAME("SEAT_AVAILABLE");
	private final String message;

	TipleMessage(String message) {
		this.message = message;
	}
}