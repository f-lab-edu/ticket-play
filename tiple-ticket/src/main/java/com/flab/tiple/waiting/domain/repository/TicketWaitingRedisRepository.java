package com.flab.tiple.waiting.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.flab.tiple.waiting.domain.model.TicketWaitingRedis;

public interface TicketWaitingRedisRepository extends CrudRepository<TicketWaitingRedis, String> {
	List<TicketWaitingRedis> findByConcertIdOrderByWaitingNumberAsc(Long concertId);
	Optional<TicketWaitingRedis> findByConcertIdAndMemberId(Long concertId, Long memberId);
	Optional<Integer> findMaxWaitingNumberByConcertId(Long concertId);
}

