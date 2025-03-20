package com.flab.tiple.concert.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConcertDetailResponseDto {
	private Long id;
	private String name;
	private String artistName;
	private String startTime;
	private String endTime;
	private String reservationStartTime;
	private String reservationEndTime;
	private Integer remainingSeat;
	private String status;
	private String concertHallName;
	private String concertHallAddress;

	@Builder
	public ConcertDetailResponseDto(Long id, String name, String artistName, String startTime, String endTime,
		String reservationStartTime, String reservationEndTime, Integer remainingSeat, String status, String concertHallName, String concertHallAddress){
		this.id = id;
		this.name = name;
		this.artistName = artistName;
		this.startTime = startTime;
		this.endTime = endTime;
		this.reservationStartTime = reservationStartTime;
		this.reservationEndTime = reservationEndTime;
		this.remainingSeat = remainingSeat;
		this.status = status;
		this.concertHallName = concertHallName;
		this.concertHallAddress = concertHallAddress;
	}
}
