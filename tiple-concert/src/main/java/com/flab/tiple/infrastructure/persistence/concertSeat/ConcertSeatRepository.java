package com.flab.tiple.infrastructure.persistence.concertSeat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flab.tiple.domain.ConcertSeat;

public interface ConcertSeatRepository extends JpaRepository<ConcertSeat, Long>, ConcertSeatRepositoryQueryDslCustom {
	/**
	 * 특정 콘서트의 사용 가능한 좌석 목록 조회
	 * @param concertId 콘서트 ID
	 * @return 사용 가능한 좌석 목록
	 */
	@Query("SELECT cs FROM ConcertSeat cs WHERE cs.concert.id = :concertId AND cs.status = 'AVAILABLE' ORDER BY cs.id")
	List<ConcertSeat> findAvailableSeatsByConcertId(@Param("concertId") Long concertId);
}
