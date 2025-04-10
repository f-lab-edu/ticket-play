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

	//PESSIMISTIC_WRITE는 데이터베이스 수준에서 쓰기 잠금(write lock)을 획득
	//JPA가 PESSIMISTIC_WRITE로 SELECT 쿼리를 실행할 때, 내부적으로는 다음과 같은 SQL을 생성
	//SELECT ... FROM table WHERE id = ? FOR UPDATE
	//여기서 FOR UPDATE 구문이 핵심입니다. 이것이 데이터베이스에게 해당 레코드에 대한 배타적 락을 요청

	//왜 SELECT 쿼리를 사용하는가?
	// 락 획득 목적: 락을 획득하기 위해서는 먼저 대상 레코드를 식별해야 합니다. 이것이 SELECT 쿼리의 목적
	// 작업흐름
	// 첫 단계: SELECT ... FOR UPDATE로 엔티티를 조회하면서 동시에 락을 획득
	// 두 번째 단계: 애플리케이션 로직에서 엔티티 상태를 변경
	// 세 번째 단계: 트랜잭션 커밋 시 변경사항이 데이터베이스에 반영됨
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "5000")})
	@Query("SELECT c FROM Concert c WHERE c.id = :id")
	Optional<Concert> findByIdWithPessimisticLock(@Param("id") Long id);
}
