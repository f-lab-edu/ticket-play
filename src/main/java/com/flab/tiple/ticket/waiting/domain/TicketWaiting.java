package com.flab.tiple.ticket.waiting.domain;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.global.entity.BaseTime;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.exception.MemberNotMatchException;
import com.flab.tiple.ticket.waiting.enums.TicketWaitingStatus;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingStatusInvalidException;

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
public class TicketWaiting extends BaseTime {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "concert_id", nullable = false)
	private Concert concert;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	private Integer waitingNumber;


	@Enumerated(EnumType.STRING)
	private TicketWaitingStatus status;

	public void checkTicketWaitingAuth() {
		if (status == TicketWaitingStatus.CANCELED) throw new TicketWaitingStatusInvalidException(ErrorCode.TICKET_WAITING_INVALID_WAITING_STATUS, ErrorCode.TICKET_WAITING_INVALID_WAITING_STATUS.getDescription());
	}

	public void checkMemberMatch(Member member) {
		if(!this.member.equals(member)) throw new MemberNotMatchException(ErrorCode.MEMBER_NOT_MATCH, ErrorCode.MEMBER_NOT_MATCH.getDescription());
	}

	public void cancel(){
		this.status = TicketWaitingStatus.CANCELED;
		super.delete();
	}

	@Builder
	public TicketWaiting(Concert concert, Member member, Integer waitingNumber, TicketWaitingStatus status){
		this.concert = concert;
		this.member = member;
		this.waitingNumber = waitingNumber;
		this.status = status;
	}

	@Override
	public String toString() {
		return "TicketWaiting{" +
			"id=" + id +
			", concert=" + concert +
			", member=" + member +
			", waitingNumber=" + waitingNumber +
			", status=" + status +
			'}';
	}
}
