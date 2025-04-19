package com.flab.tiple.ticket.waiting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flab.tiple.ticket.waiting.domain.TicketWaiting;

import jakarta.persistence.LockModeType;

public interface TicketWaitingRepository  extends JpaRepository<TicketWaiting, Long>, TicketWaitingRepositoryQueryDslCustom  {
	List<TicketWaiting> findByMemberIdOrderByCreatedAtDesc(Long memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT COALESCE(MAX(tw.waitingNumber), 0) FROM TicketWaiting tw WHERE tw.concert.id = :concertId")
	Optional<Integer> findMaxWaitingNumberByConcertIdWithLock(@Param("concertId") Long concertId);
}