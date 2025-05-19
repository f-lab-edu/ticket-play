package com.flab.tiple.ticket.reservation.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flab.tiple.ticket.reservation.domain.TicketReservation;
import com.flab.tiple.ticket.reservation.enums.TicketReservationStatus;

public interface TicketReservationRepository extends JpaRepository<TicketReservation, Long> {
	List<TicketReservation> findByMemberId(
		Long memberId
	);

	// 추가할 메소드
	Optional<TicketReservation> findBySeatIdAndMemberId(Long seatId, Long memberId);

	List<TicketReservation> findByStatusAndCreatedAtBefore(TicketReservationStatus status, LocalDateTime dateTime);

}
