package com.flab.tiple.waiting.enums;

public enum TicketWaitingStatus {
	WAITING,    // 대기 중
	NOTIFIED,   // 좌석 가용 알림 전송됨
	CANCELED,   // 취소됨
	EXPIRED     // 알림 후 시간 경과로 만료됨
}