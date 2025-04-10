package com.flab.tiple.ticket.reservation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.flab.tiple.ticket.reservation.domain.TicketReservation;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface TicketReservationRepository extends JpaRepository<TicketReservation, Long> {
	List<TicketReservation> findByMemberId(
		Long memberId
	);


	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "5000")})
	@Query("SELECT tr FROM TicketReservation tr WHERE tr.id = :id")
	Optional<TicketReservation> findByIdWithPessimisticLock(@Param("id") Long id);
}
