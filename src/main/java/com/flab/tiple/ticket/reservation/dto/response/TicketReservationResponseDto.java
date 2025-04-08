package com.flab.tiple.ticket.reservation.dto.response;

import com.flab.tiple.ticket.reservation.enums.TicketProcessStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketReservationResponseDto<T>  {

	// 처리 결과 상태
	private TicketProcessStatus status;
	// 결과 데이터 (예약 정보 또는 웨이팅 정보)
	private T data;

	@Builder
	public TicketReservationResponseDto(TicketProcessStatus status, T data) {
		this.status = status;
		this.data = data;
	}

	@Override
	public String toString() {
		return "TicketReservationResponseDto{" +
			"status=" + status +
			", data=" + data +
			'}';
	}
}
