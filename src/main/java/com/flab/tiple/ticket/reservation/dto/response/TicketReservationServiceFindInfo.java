package com.flab.tiple.ticket.reservation.dto.response;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.concert.domain.ConcertSeat;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.ticket.reservation.domain.TicketReservation;

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