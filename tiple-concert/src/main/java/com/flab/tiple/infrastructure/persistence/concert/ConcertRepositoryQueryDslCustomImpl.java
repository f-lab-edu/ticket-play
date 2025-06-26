package com.flab.tiple.infrastructure.persistence.concert;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.flab.tiple.domain
	.Concert;
import com.flab.tiple.domain.QConcert;
import com.flab.tiple.domain.enums.ConcertStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConcertRepositoryQueryDslCustomImpl implements ConcertRepositoryQueryDslCustom{

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<Concert> findAllConcerts(Pageable pageable) {
		QConcert concert = QConcert.concert;

		long total = queryFactory
			.selectFrom(concert)
			.where(concert.deletedAt.isNull())
			.fetchCount();

		List<Concert> content = queryFactory
			.selectFrom(concert)
			.where(concert.deletedAt.isNull())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(concert.createdAt.desc())
			.fetch();

		return new PageImpl<>(content, pageable, total);
	}

	@Override
	public Page<Concert> findAvailableConcerts(Pageable pageable) {
		QConcert concert = QConcert.concert;

		long total = queryFactory
			.selectFrom(concert)
			.where(
				concert.deletedAt.isNull(),
				concert.remainingSeat.gt(0),
				concert.status.ne(ConcertStatus.CLOSED)
			)
			.fetchCount();

		List<Concert> content = queryFactory
			.selectFrom(concert)
			.where(
				concert.deletedAt.isNull(),
				concert.remainingSeat.gt(0),
				concert.status.ne(ConcertStatus.CLOSED)
			)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(concert.createdAt.desc())
			.fetch();

		return new PageImpl<>(content, pageable, total);
	}
}
