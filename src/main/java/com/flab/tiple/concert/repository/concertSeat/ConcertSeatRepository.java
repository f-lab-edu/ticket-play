package com.flab.tiple.concert.repository.concertSeat;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flab.tiple.concert.domain.ConcertSeat;

public interface ConcertSeatRepository extends JpaRepository<ConcertSeat, Long>, ConcertSeatRepositoryQueryDslCustom {
}
