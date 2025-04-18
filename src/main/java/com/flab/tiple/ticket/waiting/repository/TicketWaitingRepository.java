package com.flab.tiple.ticket.waiting.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flab.tiple.ticket.waiting.domain.TicketWaiting;

public interface TicketWaitingRepository  extends JpaRepository<TicketWaiting, Long>, TicketWaitingRepositoryQueryDslCustom  {
	List<TicketWaiting> findByMemberIdOrderByCreatedAtDesc(Long memberId);
	// 기존 메서드: 콘서트별 최대 대기 번호 조회
	Optional<Integer> findMaxWaitingNumberByConcertId(Long concertId);

}