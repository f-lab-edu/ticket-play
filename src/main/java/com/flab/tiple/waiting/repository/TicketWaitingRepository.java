package com.flab.tiple.waiting.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.flab.tiple.waiting.domain.TicketWaiting;

public interface TicketWaitingRepository  extends JpaRepository<TicketWaiting, Long>, TicketWaitingRepositoryQueryDslCustom {
	List<TicketWaiting> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}