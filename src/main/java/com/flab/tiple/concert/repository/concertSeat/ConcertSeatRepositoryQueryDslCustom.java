package com.flab.tiple.concert.repository.concertSeat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.flab.tiple.concert.domain.ConcertSeat;

public interface ConcertSeatRepositoryQueryDslCustom {
	Page<ConcertSeat> findConcertSeats(Long concertId, Pageable pageable);
}
