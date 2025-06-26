package com.flab.tiple.reservation.infrastructure.message.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WaitingNumberKafkaEvent extends KafkaNotificationEvent {
	private Long concertId;
	private int waitingNumber;
	private String message;

	@Builder
	public WaitingNumberKafkaEvent(Long memberId, Long concertId, int waitingNumber, String message) {
		super("WAITING_NUMBER", memberId);
		this.concertId = concertId;
		this.waitingNumber = waitingNumber;
		this.message = message;
	}

	public static WaitingNumberKafkaEvent of(Long memberId, Long concertId, int waitingNumber) {
		String message = String.format("콘서트 #%d의 대기 번호는 %d번입니다.", concertId, waitingNumber);
		return WaitingNumberKafkaEvent.builder()
			.memberId(memberId)
			.concertId(concertId)
			.waitingNumber(waitingNumber)
			.message(message)
			.build();
	}
}