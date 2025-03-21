package com.flab.tiple.waiting.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.concert.enums.ConcertStatus;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.exception.MemberNotMatchException;
import com.flab.tiple.member.repository.MemberRepository;
import com.flab.tiple.waiting.domain.TicketWaiting;
import com.flab.tiple.waiting.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.waiting.enums.TicketWaitingStatus;
import com.flab.tiple.waiting.excpetion.TicketWaitingStatusInvalidException;
import com.flab.tiple.waiting.repository.TicketWaitingRepository;

@ExtendWith(MockitoExtension.class)
public class TicketWaitingServiceTest {

	@InjectMocks
	private TicketWaitingServiceImpl ticketWaitingService;

	@Mock
	private TicketWaitingRepository ticketWaitingRepository;

	@Mock
	private MemberRepository memberRepository;

	private Member member;
	private Concert concert;
	private TicketWaiting ticketWaiting;

	private Long waitingId = 1L;
	private Long memberId = 1L;

	@BeforeEach
	void setUp() {
		// 멤버 생성
		member = Member.builder()
			.email("test@example.com")
			.name("테스트 사용자")
			.build();
		ReflectionTestUtils.setField(member, "id", 1L);

		// 콘서트 생성 (현재 시간 기준으로 유효한 콘서트)
		concert = Concert.builder()
			.name("Shining Star 콘서트")
			.artistName("엑소")
			.startTime(LocalDateTime.now().plusHours(3))
			.endTime(LocalDateTime.now().plusHours(9))
			.reservationStartTime(LocalDateTime.now().minusDays(2))
			.reservationEndTime(LocalDateTime.now().plusDays(1))
			.remainingSeat(100)
			.status(ConcertStatus.OPEN)
			.concertHallName("서울 콘서트홀")
			.concertHallAddress("서울시 강남구")
			.concertSeatInfo("일반석, VIP석")
			.build();

		// Reflection을 사용해 ID 설정
		ReflectionTestUtils.setField(concert, "id", 1L);
		ReflectionTestUtils.setField(concert, "deletedAt", null);
		ReflectionTestUtils.setField(concert, "createdAt", LocalDateTime.now());

		ticketWaiting = TicketWaiting.builder()
			.waitingNumber(1)
			.status(TicketWaitingStatus.WAITING)
			.concert(concert)
			.member(member)
			.build();
		ReflectionTestUtils.setField(ticketWaiting, "id", 1L);

	}

	@Test
	@DisplayName("waiting 취소 성공")
	void cancelWaitingSuccess() {
		//given
		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member));
		when(ticketWaitingRepository.findById(anyLong()))
			.thenReturn(Optional.of(ticketWaiting));
		when(ticketWaitingRepository.save(any(TicketWaiting.class)))
			.thenReturn(ticketWaiting);

		//when
		TicketWaitingCancelResponseDto result = ticketWaitingService.cancelWaiting(waitingId, memberId);

		//then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getStatus()).isEqualTo(TicketWaitingStatus.CANCELED);
	}

	@Test
	@DisplayName("waiting 이미 취소된 상태일 때")
	void cancelWaitingTicketWaitingStatusInvalidException() {
		ReflectionTestUtils.setField(ticketWaiting, "status", TicketWaitingStatus.CANCELED);

		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member));
		when(ticketWaitingRepository.findById(anyLong()))
			.thenReturn(Optional.of(ticketWaiting));

		assertThrows(TicketWaitingStatusInvalidException.class, () ->
			ticketWaitingService.cancelWaiting(waitingId, memberId)
		);

		// 실제 호출 검증
		verify(ticketWaitingRepository).findById(waitingId);
		verify(memberRepository).findById(memberId);
		// 저장은 호출되지 않아야 함
		verify(ticketWaitingRepository, never()).save(any());
	}

	@Test
	@DisplayName("요청한 유저와 예약건 유저가 일치하지 않을때")
	void cancelWaitingMemberNotMatchException() {
		//given
		Member member2 = Member.builder()
			.email("test2@example.com")
			.name("테스트2 사용자")
			.build();
		ReflectionTestUtils.setField(member2, "id", 2L);

		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member2));
		when(ticketWaitingRepository.findById(anyLong()))
			.thenReturn(Optional.of(ticketWaiting));

		assertThrows(MemberNotMatchException.class, () ->
			ticketWaitingService.cancelWaiting(waitingId, 2L)
		);

		// 실제 호출 검증
		verify(ticketWaitingRepository).findById(waitingId);
		verify(memberRepository).findById(2L);
		// 저장은 호출되지 않아야 함
		verify(ticketWaitingRepository, never()).save(any());
	}
}
