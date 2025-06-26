package com.flab.tiple.reservation.application.dto.response;


import com.flab.tiple.domain.Concert;
import com.flab.tiple.domain.ConcertSeat;
import com.flab.tiple.entity.member.Member;
import com.flab.tiple.reservation.domain.model.TicketReservation;

import lombok.Builder;
import lombok.Getter;

@Getter
public class TicketReservationServiceFindInfo{
	private Concert concert;
	private Member member;
	private ConcertSeat concertSeat;
	private TicketReservation ticketReservation;

		@Builder
		public TicketReservationServiceFindInfo(Concert concert, Member member, TicketReservation ticketReservation, ConcertSeat concertSeat) {
		this.concert = concert;
		this.member = member;
		this.ticketReservation = ticketReservation;
		this.concertSeat = concertSeat;
	}
}