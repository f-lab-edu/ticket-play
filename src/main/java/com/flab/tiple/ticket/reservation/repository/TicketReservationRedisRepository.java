package com.flab.tiple.ticket.reservation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.flab.tiple.ticket.reservation.domain.TicketReservationRedis;

public interface TicketReservationRedisRepository extends CrudRepository<TicketReservationRedis, String> {
	List<TicketReservationRedis> findByMemberId(Long memberId);
	Optional<TicketReservationRedis> findBySeatId(Long seatId);
}