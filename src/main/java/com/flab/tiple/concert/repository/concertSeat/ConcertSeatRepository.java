package com.flab.tiple.concert.repository.concertSeat;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.flab.tiple.concert.domain.ConcertSeat;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface ConcertSeatRepository extends JpaRepository<ConcertSeat, Long>, ConcertSeatRepositoryQueryDslCustom {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "5000")})
	@Query("SELECT cs FROM ConcertSeat cs WHERE cs.id = :id")
	Optional<ConcertSeat> findByIdWithPessimisticLock(@Param("id") Long id);
}
