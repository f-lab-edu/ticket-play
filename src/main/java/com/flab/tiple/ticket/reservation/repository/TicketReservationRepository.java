package com.flab.tiple.ticket.reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flab.tiple.ticket.reservation.domain.TicketReservation;

public interface TicketReservationRepository extends JpaRepository<TicketReservation, Long> {
	List<TicketReservation> findByMemberId(
		Long memberId
	);
}
