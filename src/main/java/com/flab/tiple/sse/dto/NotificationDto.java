package com.flab.tiple.sse.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationDto {
	private String message;
	private Long seatId;
	private Long concertId;
	private LocalDateTime timestamp = LocalDateTime.now();
}