package com.flab.tiple.reservation.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.flab.tiple.reservation.domain.model.TicketReservationRedis;

public interface TicketReservationRedisRepository extends CrudRepository<TicketReservationRedis, String> {
	List<TicketReservationRedis> findByMemberId(Long memberId);
	Optional<TicketReservationRedis> findBySeatId(Long seatId);
}