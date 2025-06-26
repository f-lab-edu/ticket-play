package com.flab.tiple.reservation.application.dto.response;

import com.flab.tiple.domain.enums.ConcertSeatGrade;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConcertSeatInfo {
	private Long id;
	private ConcertSeatGrade grade;
	private Integer seatNumber;

	@Builder
	public ConcertSeatInfo(Long id, ConcertSeatGrade grade, Integer seatNumber) {
		this.id = id;
		this.grade = grade;
		this.seatNumber = seatNumber;
	}

}