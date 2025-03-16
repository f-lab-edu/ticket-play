package com.flab.tiple.concert.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConcertSeatResponseDto {
	private Long id;
	private Long concertId;
	private String grade;
	private Integer seatNumber;
//	private Boolean isAvailable; //예약가능여부 => 추 후 티켓예매 기능추가와 함께 추가예정

	@Builder
	public ConcertSeatResponseDto(Long id, Long concertId, String grade, Integer seatNumber) {
		this.id = id;
		this.concertId = concertId;
		this.grade = grade;
		this.seatNumber = seatNumber;
	//	this.isAvailable = isAvailable;
	}
}
