package com.flab.tiple.ticket.reservation.dto.message;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * kafka연결 테스트시 사용한 코드로 추 후 수정될 확률 높음
 */
@Builder
@AllArgsConstructor
@Data
@ToString
public class TicketReservationResultMessage {
	private String userId;
	private String eventId;
	private int quantity;
	private boolean success;
	private String message;
	private LocalDateTime processedTime;

}
