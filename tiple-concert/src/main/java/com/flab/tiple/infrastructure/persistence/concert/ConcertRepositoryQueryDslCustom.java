package com.flab.tiple.infrastructure.persistence.concert;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.flab.tiple.domain.Concert;

public interface ConcertRepositoryQueryDslCustom {
	Page<Concert> findAllConcerts(Pageable pageable);
	Page<Concert> findAvailableConcerts(Pageable pageable);
}
