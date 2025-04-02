package com.flab.tiple.ticket.waiting.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flab.tiple.ticket.waiting.domain.TicketWaiting;

public interface TicketWaitingRepository  extends JpaRepository<TicketWaiting, Long> {
	List<TicketWaiting> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}