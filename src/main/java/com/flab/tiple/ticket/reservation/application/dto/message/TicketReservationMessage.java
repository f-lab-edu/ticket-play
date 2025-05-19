package com.flab.tiple.ticket.reservation.application.dto.message;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * kafka연결 테스트시 사용한 코드로 추 후 수정될 확률 높음
 */
@RequiredArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class TicketReservationMessage {
	private String userId;
	private String eventId;
	private int quantity;
	private LocalDateTime requestTime;
}
