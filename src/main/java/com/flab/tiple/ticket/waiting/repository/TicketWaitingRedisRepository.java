package com.flab.tiple.ticket.waiting.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.flab.tiple.ticket.waiting.domain.TicketWaitingRedis;

public interface TicketWaitingRedisRepository extends CrudRepository<TicketWaitingRedis, String> {
	List<TicketWaitingRedis> findByConcertIdOrderByWaitingNumberAsc(Long concertId);
	Optional<TicketWaitingRedis> findByConcertIdAndMemberId(Long concertId, Long memberId);
	Optional<Integer> findMaxWaitingNumberByConcertId(Long concertId);
}
