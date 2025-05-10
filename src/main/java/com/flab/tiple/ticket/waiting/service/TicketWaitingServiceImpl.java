package com.flab.tiple.ticket.waiting.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.exception.MemberNotFoundException;
import com.flab.tiple.member.repository.MemberRepository;
import com.flab.tiple.ticket.waiting.domain.TicketWaiting;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingInfoResponseDto;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingNotFoundException;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRepository;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketWaitingServiceImpl implements TicketWaitingService {

	private final TicketWaitingRepository ticketWaitingRepository;
	private final MemberRepository memberRepository;

	@Override
	@Transactional
	public TicketWaitingCancelResponseDto cancelWaiting(Long waitingId, Long memberId) {
		// 1. 정보 찾기
		TicketWaitingServiceFindInfo info = findWaitingInfo(waitingId, memberId);
		// 2. 정보 검증
		validateWaitingCancellation(info);
		// 3. 정보 수정
		return processWaitingCancellation(info);
	}

	@Override
	public List<TicketWaitingInfoResponseDto> getMemberWaitingList(Long memberId) {
		List<TicketWaiting> waitingList = ticketWaitingRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

		return waitingList.stream()
			.map(this::TicketWaitingResponseToDto)
			.collect(Collectors.toList());
	}

	// 1. 정보 찾기 메소드
	private TicketWaitingServiceFindInfo findWaitingInfo(Long waitingId, Long memberId) {
		// 웨이팅 정보 조회
		TicketWaiting waiting = ticketWaitingRepository.findById(waitingId)
			.orElseThrow(() -> new TicketWaitingNotFoundException(ErrorCode.TICKET_WAITING_NOT_FOUND,
				ErrorCode.TICKET_WAITING_NOT_FOUND.getDescription()));

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND,
				ErrorCode.MEMBER_NOT_FOUND.getDescription()));

		return TicketWaitingServiceFindInfo.builder()
			.ticketWaiting(waiting)
			.member(member)
			.build();
	}

	// 2. 정보 검증 메소드
	private void validateWaitingCancellation(TicketWaitingServiceFindInfo info) {
		// 웨이팅 상태 검증
		info.getTicketWaiting().checkTicketWaitingAuth();
		// 사용자 일치 검증
		info.getTicketWaiting().checkMemberMatch(info.getMember());
	}

	// 3. 정보 수정 메소드
	private TicketWaitingCancelResponseDto processWaitingCancellation(TicketWaitingServiceFindInfo info) {
		// 상태 변경
		info.getTicketWaiting().cancel();
		// 저장 및 반환
		TicketWaiting savedWaiting = ticketWaitingRepository.save(info.getTicketWaiting());
		return TicketWaitingCancleResponseDto(savedWaiting);
	}

	private TicketWaitingInfoResponseDto TicketWaitingResponseToDto(TicketWaiting ticketWaiting) {
		Concert ticketConcert = ticketWaiting.getConcert();
		return TicketWaitingInfoResponseDto.builder()
			.concertId(ticketConcert.getId())
			.concertName(ticketConcert.getName())
			.waitingNumber(ticketWaiting.getWaitingNumber())
			.status(ticketWaiting.getStatus().name())
			.build();
	}

	private TicketWaitingCancelResponseDto TicketWaitingCancleResponseDto(TicketWaiting ticketWaiting) {
		return TicketWaitingCancelResponseDto.builder()
			.id(ticketWaiting.getId())
			.status(ticketWaiting.getStatus())
			.deletedAt(ticketWaiting.getDeletedAt().toString())
			.build();
	}

	@Getter
	static class TicketWaitingServiceFindInfo {
		private TicketWaiting ticketWaiting;
		private Member member;

		@Builder
		public TicketWaitingServiceFindInfo(TicketWaiting ticketWaiting, Member member) {
			this.ticketWaiting = ticketWaiting;
			this.member = member;
		}
	}
}