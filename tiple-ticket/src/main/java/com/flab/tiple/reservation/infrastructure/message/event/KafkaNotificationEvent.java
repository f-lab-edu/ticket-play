package com.flab.tiple.reservation.infrastructure.message.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.LocalDateTime;

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.PROPERTY,
	property = "type"
)
@JsonSubTypes({
	@JsonSubTypes.Type(value = ReservationStatusKafkaEvent.class, name = "RESERVATION_STATUS"),
	@JsonSubTypes.Type(value = WaitingNumberKafkaEvent.class, name = "WAITING_NUMBER"),
	@JsonSubTypes.Type(value = SeatAvailableKafkaEvent.class, name = "SEAT_AVAILABLE")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class KafkaNotificationEvent {
	private String eventId;
	private String type;
	private Long memberId;
	private LocalDateTime timestamp;
	private int retryCount;

	public KafkaNotificationEvent(String type, Long memberId) {
		this.eventId = java.util.UUID.randomUUID().toString();
		this.type = type;
		this.memberId = memberId;
		this.timestamp = LocalDateTime.now();
		this.retryCount = 0;
	}

	public void incrementRetryCount() {
		this.retryCount++;
	}
}