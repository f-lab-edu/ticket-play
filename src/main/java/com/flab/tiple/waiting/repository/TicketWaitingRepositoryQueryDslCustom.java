package com.flab.tiple.waiting.repository;

import java.util.List;
import java.util.Optional;
import com.flab.tiple.waiting.enums.TicketWaitingStatus;

public interface TicketWaitingRepositoryQueryDslCustom {

	 // 콘서트별 최대 대기 번호 조회
	Optional<Integer> findMaxWaitingNumberByConcertId(Long concertId);

	 // 특정 회원의 대기 중인 콘서트 목록 조회
	List<Long> findConcertIdsByMemberIdAndStatus(Long memberId, TicketWaitingStatus status);

}
