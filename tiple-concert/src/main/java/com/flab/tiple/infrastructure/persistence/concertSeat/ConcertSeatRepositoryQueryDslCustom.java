package com.flab.tiple.infrastructure.persistence.concertSeat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.flab.tiple.domain.ConcertSeat;

public interface ConcertSeatRepositoryQueryDslCustom {
	Page<ConcertSeat> findConcertSeats(Long concertId, Pageable pageable);
}
