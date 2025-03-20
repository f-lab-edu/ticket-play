package com.flab.tiple.concert.repository.concertSeat;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.flab.tiple.concert.domain.ConcertSeat;
import com.flab.tiple.concert.domain.QConcertSeat;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConcertSeatRepositoryQueryDslCustomImpl implements ConcertSeatRepositoryQueryDslCustom{

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<ConcertSeat> findConcertSeats(Long concertId, Pageable pageable) {
		QConcertSeat seat = QConcertSeat.concertSeat;

		// 전체 카운트 쿼리
		long total = queryFactory
			.selectFrom(seat)
			.where(
				seat.concert.id.eq(concertId),
				seat.deletedAt.isNull()
			)
			.fetchCount();

		// 페이징 쿼리
		List<ConcertSeat> content = queryFactory
			.selectFrom(seat)
			.where(
				seat.concert.id.eq(concertId),
				seat.deletedAt.isNull()
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(seat.seatNumber.desc())
			.fetch();

		return new PageImpl<>(content, pageable, total);
	}
}
