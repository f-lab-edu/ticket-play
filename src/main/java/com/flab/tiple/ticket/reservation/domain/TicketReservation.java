package com.flab.tiple.ticket.reservation.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.flab.tiple.concert.domain.ConcertSeat;
import com.flab.tiple.global.entity.BaseTime;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.exception.MemberNotMatchException;
import com.flab.tiple.ticket.reservation.enums.TicketReservationStatus;
import com.flab.tiple.ticket.reservation.exception.TicketReservationStatusException;

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
import lombok.ToString;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class TicketReservation extends BaseTime {

	// 상태 전이 규칙을 정의하는 정적 맵
	private static final Map<TicketReservationStatus, Set<TicketReservationStatus>> POSSIBLE_STATUS_TRANSITIONS = new HashMap<>();


	// 정적 맵을 설정하는 메소드 (외부에서 호출됨)
	public static void initializeStatusTransitions(Map<TicketReservationStatus, Set<TicketReservationStatus>> transitionsMap) {
		POSSIBLE_STATUS_TRANSITIONS.clear();
		POSSIBLE_STATUS_TRANSITIONS.putAll(transitionsMap);
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seat_id")
	private ConcertSeat seat;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TicketReservationStatus status;

	/**
	 * 상태 전이 가능 여부를 검증
	 * @param targetStatus 전환하려는 목표 상태
	 * @throws TicketReservationStatusException 상태 전이가 불가능한 경우
	 */
	public void checkPossibleStatus(TicketReservationStatus targetStatus) {
		Set<TicketReservationStatus> possibleSourceStatuses = POSSIBLE_STATUS_TRANSITIONS.get(targetStatus);

		// 목표 상태로의 전이 규칙이 없거나, 현재 상태에서 목표 상태로 전이가 불가능한 경우
		if (possibleSourceStatuses == null || !possibleSourceStatuses.contains(this.status)) {
			ErrorCode errorCode = ErrorCode.TICKET_RESERVATION_NOT_POSSIBLE_STATUS;
			if (targetStatus == TicketReservationStatus.CANCELLED) {
				errorCode = ErrorCode.TICKET_RESERVATION_NOT_CANCEL_STATUS;
			}
			throw new TicketReservationStatusException(errorCode, errorCode.getDescription());
		}
	}

	public void timeout() {
		this.status = TicketReservationStatus.TIMEOUT;
	}

	public void checkMatchMember(Member member) {
		if (!this.member.equals(member)) {
			throw new MemberNotMatchException(ErrorCode.MEMBER_NOT_MATCH,
				ErrorCode.MEMBER_NOT_MATCH.getDescription());
		}
	}

	// 기존의 특정 상태 검증 메소드들을 통합된 메소드로 대체
	public void checkStatus() {
		checkPossibleStatus(TicketReservationStatus.APPROVED);
	}

	public void checkCancelPossibleStatus() {
		checkPossibleStatus(TicketReservationStatus.CANCELLED);
	}

	public void approve() {
		checkPossibleStatus(TicketReservationStatus.APPROVED);
		this.status = TicketReservationStatus.APPROVED;
	}

	public void cancel() {
		checkPossibleStatus(TicketReservationStatus.CANCELLED);
		this.status = TicketReservationStatus.CANCELLED;
	}

	@Builder
	public TicketReservation(Member member, ConcertSeat seat, TicketReservationStatus status) {
		this.member = member;
		this.seat = seat;
		this.status = status;
	}

}