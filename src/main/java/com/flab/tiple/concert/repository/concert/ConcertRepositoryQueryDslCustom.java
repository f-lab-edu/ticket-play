package com.flab.tiple.concert.repository.concert;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.flab.tiple.concert.domain.Concert;

public interface ConcertRepositoryQueryDslCustom {
	Page<Concert> findAllConcerts(Pageable pageable);
	Page<Concert> findAvailableConcerts(Pageable pageable);
}
