package com.flab.tiple.ticket.domain;

import com.flab.tiple.concert.domain.ConcertSeat;
import com.flab.tiple.global.entity.BaseTime;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.exception.MemberNotMatchException;
import com.flab.tiple.ticket.enums.TicketReservationStatus;
import com.flab.tiple.ticket.exception.TicketReservationStatusException;

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
public class TicketReservation extends BaseTime {
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

	public void checkMatchMember(Member member){
		if(!this.member.equals(member)) throw new MemberNotMatchException(ErrorCode.MEMBER_NOT_MATCH, ErrorCode.MEMBER_NOT_MATCH.getDescription());
	}

	public void checkStatus(){
		if(this.status != TicketReservationStatus.PENDING) throw new TicketReservationStatusException(ErrorCode.TICKET_RESERVATION_NOT_POSSIBLE_STATUS, ErrorCode.TICKET_RESERVATION_NOT_POSSIBLE_STATUS.getDescription());
	}

	public void checkCancelPossibleStatus(){
		if (this.getStatus() != TicketReservationStatus.PENDING
			&& this.getStatus() != TicketReservationStatus.APPROVED) {
			throw new TicketReservationStatusException(ErrorCode.TICKET_RESERVATION_NOT_CANCEL_STATUS, ErrorCode.TICKET_RESERVATION_NOT_CANCEL_STATUS.getDescription());
		}
	}

	public void approve(){
		this.status = TicketReservationStatus.APPROVED;
	}

	public void cancel(){
		this.status = TicketReservationStatus.CANCELLED;
	}

	@Builder
	public TicketReservation(Member member, ConcertSeat seat,TicketReservationStatus status) {
		this.member = member;
		this.seat = seat;
		this.status = status;
	}

	@Override
	public String toString() {
		return "TicketReservation{" +
			"id=" + id +
			", member=" + member +
			", seat=" + seat +
			", status=" + status +
			'}';
	}
}

