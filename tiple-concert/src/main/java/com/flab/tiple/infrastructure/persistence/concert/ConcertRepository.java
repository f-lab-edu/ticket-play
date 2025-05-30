package com.flab.tiple.infrastructure.persistence.concert;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flab.tiple.domain.Concert;
import com.flab.tiple.domain.enums.ConcertStatus;

public interface ConcertRepository extends JpaRepository<Concert, Long>, ConcertRepositoryQueryDslCustom {
	Optional<Concert> findByIdAndDeletedAtIsNull(Long id);
	/**
	 * 콘서트 상태별로 콘서트 목록 조회
	 * @param status 조회할 콘서트 상태 (예: OPEN, CLOSED, CANCELLED)
	 * @return 해당 상태의 콘서트 목록
	 */
	List<Concert> findByStatus(ConcertStatus status);
}
