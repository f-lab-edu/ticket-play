package com.flab.tiple.reservation.infrastructure.message.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatAvailableKafkaEvent extends KafkaNotificationEvent {
	private Long seatId;
	private Long concertId;
	private String message;

	@Builder
	public SeatAvailableKafkaEvent(Long memberId, Long seatId, Long concertId, String message) {
		super("SEAT_AVAILABLE", memberId);
		this.seatId = seatId;
		this.concertId = concertId;
		this.message = message;
	}

	public static SeatAvailableKafkaEvent of(Long memberId, Long seatId, Long concertId, String message) {
		return SeatAvailableKafkaEvent.builder()
			.memberId(memberId)
			.seatId(seatId)
			.concertId(concertId)
			.message(message)
			.build();
	}
}