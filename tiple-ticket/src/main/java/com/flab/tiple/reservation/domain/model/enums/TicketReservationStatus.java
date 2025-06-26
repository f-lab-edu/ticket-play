package com.flab.tiple.reservation.domain.model.enums;

public enum TicketReservationStatus {
	PENDING,   // 좌석 예약 대기 상태
	APPROVED,  // 예매 확정 상태
	TIMEOUT,   // 예매 시간 만료 상태
	CANCELLED  // 예매 취소 상태
}