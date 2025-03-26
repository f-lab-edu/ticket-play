package com.flab.tiple.concert.domain;

import java.time.LocalDateTime;

import com.flab.tiple.concert.enums.ConcertStatus;
import com.flab.tiple.concert.exception.ConcertCancelTimeException;
import com.flab.tiple.concert.exception.ConcertClosedException;
import com.flab.tiple.concert.exception.ConcertEndTimeException;
import com.flab.tiple.concert.exception.ConcertNotStartDateException;
import com.flab.tiple.global.entity.BaseTime;
import com.flab.tiple.global.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concert extends BaseTime {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, length = 20)
	private String artistName;

	@Column(nullable = false)
	private LocalDateTime startTime;

	@Column(nullable = false)
	private LocalDateTime endTime;

	@Column(nullable = false)
	private LocalDateTime reservationStartTime;

	@Column(nullable = false)
	private LocalDateTime reservationEndTime;

	@Column(nullable = false)
	private Integer remainingSeat;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ConcertStatus status;

	@Column(nullable = false, length = 30)
	private String concertHallName;

	@Column(nullable = false, length = 200)
	private String concertHallAddress;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String concertSeatInfo;

	public void reservationStatusCheck() {
		if(this.status != ConcertStatus.OPEN) throw new ConcertClosedException(ErrorCode.CONCERT_CLOSED, ErrorCode.CONCERT_CLOSED.getDescription());
		if(this.reservationStartTime.isAfter(LocalDateTime.now())) throw new ConcertNotStartDateException(ErrorCode.CONCERT_NOT_START_TIME_RESERVATION, ErrorCode.CONCERT_NOT_START_TIME_RESERVATION.getDescription());
		if(this.reservationEndTime.isBefore(LocalDateTime.now())) throw new ConcertEndTimeException(ErrorCode.CONCERT_LIMIT_END_TIME_RESERVATION, ErrorCode.CONCERT_LIMIT_END_TIME_RESERVATION.getDescription());
	}

	public void cancelTimeAvailableCheck(){
		if (this.startTime.minusHours(24).isBefore(LocalDateTime.now()))
			throw new ConcertCancelTimeException(ErrorCode.CONCERT_CANCEL_TIME_EXCEED, ErrorCode.CONCERT_CANCEL_TIME_EXCEED.getDescription());
	}

	public void reserveSeat() {
		this.remainingSeat--;
	}

	public void cancelSeat(){
		this.remainingSeat++;
	}

	@Builder
	public Concert(String name, String artistName, LocalDateTime startTime, LocalDateTime endTime,
		LocalDateTime reservationStartTime, LocalDateTime reservationEndTime
		, Integer remainingSeat, ConcertStatus status, String concertHallName, String concertHallAddress, String concertSeatInfo) {
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

	@Override
	public String toString() {
		return "Concert{" +
			"id=" + id +
			", name='" + name + '\'' +
			", artistName='" + artistName + '\'' +
			", startTime=" + startTime +
			", endTime=" + endTime +
			", reservationStartTime=" + reservationStartTime +
			", reservationEndTime=" + reservationEndTime +
			", remainingSeat=" + remainingSeat +
			", status=" + status +
			", concertHallName='" + concertHallName + '\'' +
			", concertHallAddress='" + concertHallAddress + '\'' +
			", concertSeatInfo='" + concertSeatInfo + '\'' +
			'}';
	}
}