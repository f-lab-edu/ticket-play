package com.flab.tiple.ticket.dto.response;


import com.flab.tiple.concert.dto.response.ConcertSeatInfo;
import com.flab.tiple.member.dto.response.MemberInfoDto;
import com.flab.tiple.ticket.enums.TicketReservationStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TicketReservationInfoResponseDto {
	private Long id;
	private MemberInfoDto memberInfo;
	private ConcertSeatInfo seatInfo;
	private TicketReservationStatus status;
	private String createdAt;

	@Builder
	public TicketReservationInfoResponseDto(Long id, MemberInfoDto memberInfo, ConcertSeatInfo seatInfo, TicketReservationStatus status, String createdAt) {
		this.id = id;
		this.memberInfo = memberInfo;
		this.seatInfo = seatInfo;
		this.status = status;
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		return "TicketReservationResponseDto{" +
			"id=" + id +
			", memberInfo=" + memberInfo +
			", seatInfo=" + seatInfo +
			", status=" + status +
			", createdAt='" + createdAt + '\'' +
			'}';
	}
}

