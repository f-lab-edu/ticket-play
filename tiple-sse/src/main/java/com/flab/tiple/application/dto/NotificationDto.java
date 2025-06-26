package com.flab.tiple.application.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationDto {
	private String message;
	private Long seatId;
	private Long concertId;
	private LocalDateTime timestamp;

	public static NotificationDto of(String message, Long seatId, Long concertId) {
		return NotificationDto.builder()
			.message(message)
			.seatId(seatId)
			.concertId(concertId)
			.timestamp(LocalDateTime.now())
			.build();
	}
}