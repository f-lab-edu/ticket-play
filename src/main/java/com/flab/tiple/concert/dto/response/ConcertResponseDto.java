package com.flab.tiple.concert.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConcertResponseDto {
	private Long id;
	private String name;
	private String artistName;
	private String status;
	private String startTime;
	private String endTime;
	private String reservationStartTime;
	private String reservationEndTime;

	@Builder
	public ConcertResponseDto(Long id, String name, String status, String artistName, String startTime, String endTime, String reservationStartTime, String reservationEndTime) {
		this.id = id;
		this.name = name;
		this.artistName = artistName;
		this.status = status;
		this.startTime = startTime;
		this.endTime = endTime;
		this.reservationStartTime = reservationStartTime;
		this.reservationEndTime = reservationEndTime;
	}
}
