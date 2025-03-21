package com.flab.tiple.waiting.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.exception.MemberNotFoundException;
import com.flab.tiple.member.repository.MemberRepository;
import com.flab.tiple.waiting.domain.TicketWaiting;
import com.flab.tiple.waiting.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.waiting.dto.response.TicketWaitingInfoResponseDto;
import com.flab.tiple.waiting.excpetion.TicketWaitingNotFoundException;
import com.flab.tiple.waiting.repository.TicketWaitingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketWaitingServiceImpl implements TicketWaitingService {

	private final TicketWaitingRepository ticketWaitingRepository;
	private final MemberRepository memberRepository;

	@Override
	public TicketWaitingCancelResponseDto cancelWaiting(Long waitingId, Long memberId) {
		// 웨이팅 정보 조회
		TicketWaiting waiting = ticketWaitingRepository.findById(waitingId)
			.orElseThrow(() -> new TicketWaitingNotFoundException(ErrorCode.TICKET_WAITING_NOT_FOUND,
				ErrorCode.TICKET_WAITING_NOT_FOUND.getDescription()));

		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND,ErrorCode.MEMBER_NOT_FOUND.getDescription()));

		waiting.checkTicketWaitingAuth();

		waiting.checkMemberMatch(member);
		// 상태 변경
		waiting.cancel();

		TicketWaiting savedWaiting = ticketWaitingRepository.save(waiting);

		return TicketWaitingCancleResponseDto(savedWaiting);
	}

	@Override
	public List<TicketWaitingInfoResponseDto> getMemberWaitingList(Long memberId) {
		List<TicketWaiting> waitingList = ticketWaitingRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

		return waitingList.stream()
			.map(this::TicketWaitingResponseToDto)
			.collect(Collectors.toList());
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
}
