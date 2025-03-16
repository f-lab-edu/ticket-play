package com.flab.tiple.concert.repository.concert;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flab.tiple.concert.domain.Concert;

public interface ConcertRepository extends JpaRepository<Concert, Long>, ConcertRepositoryQueryDslCustom {
	Optional<Concert> findByIdAndDeletedAtIsNull(Long id);
}
