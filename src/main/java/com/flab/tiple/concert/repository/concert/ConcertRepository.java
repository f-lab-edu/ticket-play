package com.flab.tiple.concert.repository.concert;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.flab.tiple.concert.domain.Concert;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface ConcertRepository extends JpaRepository<Concert, Long>, ConcertRepositoryQueryDslCustom {
	Optional<Concert> findByIdAndDeletedAtIsNull(Long id);

	@Query("SELECT c FROM Concert c WHERE c.id = :id")
	@QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "5000")})
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Concert> findByIdForUpdate(@Param("id") Long id);
}
