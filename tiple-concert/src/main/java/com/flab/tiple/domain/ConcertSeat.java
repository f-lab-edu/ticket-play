package com.flab.tiple.domain;


import com.flab.tiple.domain.enums.ConcertSeatGrade;
import com.flab.tiple.domain.enums.SeatStatus;
import com.flab.tiple.domain.exception.ConcertSeatReservationException;
import com.flab.tiple.entity.BaseTime;
import com.flab.tiple.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertSeat extends BaseTime {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "concert_id")
	private Concert concert;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ConcertSeatGrade grade;

	private Integer seatNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private SeatStatus status;

	// 좌석 예약 가능 여부 검증 메서드
	public void validateReservation() {
		// 좌석 상태 검증
		if (this.status != SeatStatus.AVAILABLE) {
			throw new ConcertSeatReservationException(ErrorCode.CONCERT_SEAT_RESERVATION_NOT_POSSIBLE, ErrorCode.CONCERT_SEAT_RESERVATION_NOT_POSSIBLE.getDescription());
		}
	}

	// 좌석 예약 메서드
	public void pending() {
		validateReservation(); // 예약 전 검증
		this.status = SeatStatus.PENDING;
	}

	// 좌석 예약 승인 메서드
	public void reserve() {
		this.status = SeatStatus.APPROVED;
	}

	// 좌석 예약 취소 메서드
	public void cancel() {
		this.status = SeatStatus.AVAILABLE;
	}

	@Builder
	public ConcertSeat(Concert concert, ConcertSeatGrade grade, Integer seatNumber, SeatStatus status){
		this.concert = concert;
		this.grade = grade;
		this.seatNumber = seatNumber;
		this.status = status;
	}

}
