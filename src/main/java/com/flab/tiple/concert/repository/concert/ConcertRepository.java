package com.flab.tiple.concert.repository.concert;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.concert.enums.ConcertStatus;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface ConcertRepository extends JpaRepository<Concert, Long>, ConcertRepositoryQueryDslCustom {
	Optional<Concert> findByIdAndDeletedAtIsNull(Long id);
	/**
	 * 콘서트 상태별로 콘서트 목록 조회
	 * @param status 조회할 콘서트 상태 (예: OPEN, CLOSED, CANCELLED)
	 * @return 해당 상태의 콘서트 목록
	 */
	List<Concert> findByStatus(ConcertStatus status);
}
