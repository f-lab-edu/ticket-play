package com.flab.tiple.reservation.infrastructure.message.event;

import com.flab.tiple.reservation.domain.model.enums.TicketReservationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusKafkaEvent extends KafkaNotificationEvent {
	private Long reservationId;
	private TicketReservationStatus status;
	private String message;

	@Builder
	public ReservationStatusKafkaEvent(Long memberId, Long reservationId,
		TicketReservationStatus status, String message) {
		super("RESERVATION_STATUS", memberId);
		this.reservationId = reservationId;
		this.status = status;
		this.message = message;
	}

	public static ReservationStatusKafkaEvent of(Long memberId, Long reservationId, TicketReservationStatus status) {
		String message = String.format("예약 #%d의 상태가 [%s]로 변경되었습니다.", reservationId, status.name());
		return ReservationStatusKafkaEvent.builder()
			.memberId(memberId)
			.reservationId(reservationId)
			.status(status)
			.message(message)
			.build();
	}
}