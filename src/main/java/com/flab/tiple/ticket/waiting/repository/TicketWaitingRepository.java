package com.flab.tiple.ticket.waiting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flab.tiple.ticket.waiting.domain.TicketWaiting;

public interface TicketWaitingRepository  extends JpaRepository<TicketWaiting, Long>, TicketWaitingRepositoryQueryDslCustom  {
	List<TicketWaiting> findByMemberIdOrderByCreatedAtDesc(Long memberId);
	Optional<Integer> findMaxWaitingNumberByConcertId(Long concertId);
}