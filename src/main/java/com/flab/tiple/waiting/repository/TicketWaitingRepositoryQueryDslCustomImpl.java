package com.flab.tiple.waiting.repository;

import java.util.List;
import java.util.Optional;

import com.flab.tiple.waiting.domain.QTicketWaiting;
import com.flab.tiple.waiting.enums.TicketWaitingStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TicketWaitingRepositoryQueryDslCustomImpl implements TicketWaitingRepositoryQueryDslCustom {
	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<Integer> findMaxWaitingNumberByConcertId(Long concertId) {
		QTicketWaiting ticketWaiting = QTicketWaiting.ticketWaiting;
		Integer maxNumber = queryFactory
			.select(ticketWaiting.waitingNumber.max())
			.from(ticketWaiting)
			.where(ticketWaiting.concert.id.eq(concertId)
				.and(ticketWaiting.deletedAt.isNull()))
			.fetchOne();

		return Optional.ofNullable(maxNumber);
	}

	@Override
	public List<Long> findConcertIdsByMemberIdAndStatus(Long memberId, TicketWaitingStatus status) {
		QTicketWaiting ticketWaiting = QTicketWaiting.ticketWaiting;
		return queryFactory
			.select(ticketWaiting.concert.id)
			.from(ticketWaiting)
			.where(ticketWaiting.member.id.eq(memberId)
				.and(ticketWaiting.status.eq(status))
				.and(ticketWaiting.deletedAt.isNull()))
			.fetch();
	}
}
