package com.flab.tiple.ticket.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;

import java.time.LocalDateTime;
import java.util.List;
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
import com.flab.tiple.concert.domain.ConcertSeat;
import com.flab.tiple.concert.enums.ConcertSeatGrade;
import com.flab.tiple.concert.enums.ConcertStatus;
import com.flab.tiple.concert.enums.SeatStatus;
import com.flab.tiple.concert.exception.ConcertCancelTimeException;
import com.flab.tiple.concert.exception.ConcertClosedException;
import com.flab.tiple.concert.exception.ConcertEndTimeException;
import com.flab.tiple.concert.exception.ConcertNotStartDateException;
import com.flab.tiple.concert.repository.concert.ConcertRepository;
import com.flab.tiple.concert.repository.concertSeat.ConcertSeatRepository;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.exception.MemberNotMatchException;
import com.flab.tiple.member.repository.MemberRepository;
import com.flab.tiple.ticket.domain.TicketReservation;
import com.flab.tiple.ticket.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.dto.response.TicketReservationResponseDto;
import com.flab.tiple.ticket.enums.TicketReservationStatus;
import com.flab.tiple.ticket.exception.TicketReservationStatusException;
import com.flab.tiple.ticket.repository.TicketReservationRepository;

@ExtendWith(MockitoExtension.class)
public class TicketReservationServiceTest {
	@Mock
	private TicketReservationRepository ticketReservationRepository;

	@Mock
	private ConcertSeatRepository concertSeatRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private ConcertRepository concertRepository;

	@InjectMocks
	private TicketReservationService ticketReservationService;

	private Member member;
	private ConcertSeat concertSeat;
	private Concert concert;
	private TicketReservation ticketReservation;

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

		// 좌석 생성
		concertSeat = ConcertSeat.builder()
			.concert(concert)
			.seatNumber(1)
			.grade(ConcertSeatGrade.A)
			.status(SeatStatus.AVAILABLE)
			.build();
		ReflectionTestUtils.setField(concertSeat, "id", 1L);

		// 티켓 예약 생성
		ticketReservation = TicketReservation.builder()
			.member(member)
			.seat(concertSeat)
			.status(TicketReservationStatus.PENDING)
			.build();
		ReflectionTestUtils.setField(ticketReservation, "id", 1L);
		ReflectionTestUtils.setField(ticketReservation, "createdAt", LocalDateTime.now());
	}

	@Test
	@DisplayName("예약 성공 테스트")
	void requestReservationSuccess() {
		// Given
		TicketReservationRequestDto requestDto = TicketReservationRequestDto.builder()
			.seatId(1L)
			.build();

		// 리포지토리 모킹
		when(concertSeatRepository.findById(anyLong()))
			.thenReturn(Optional.of(concertSeat));
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member));
		when(ticketReservationRepository.save(any(TicketReservation.class)))
			.thenReturn(ticketReservation);

		// When
		TicketReservationResponseDto responseDto = ticketReservationService
			.requestReservation(requestDto, member.getId());

		//then
		assertNotNull(responseDto);
		assertEquals(member.getEmail(), responseDto.getMemberInfo().getEmail());
		assertEquals(concertSeat.getSeatNumber(), responseDto.getSeatInfo().getSeatNumber());
	}


	@Test
	@DisplayName("concert 상태 관련 예외 테스트")
	void concertStatusExceptionCheck() {
		// Given
		TicketReservationRequestDto requestDto = TicketReservationRequestDto.builder()
			.seatId(1L)
			.build();

		ReflectionTestUtils.setField(concert, "status", ConcertStatus.CLOSED);

		// 리포지토리 모킹
		when(concertSeatRepository.findById(anyLong()))
			.thenReturn(Optional.of(concertSeat));
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member));

		// When & Then
		assertThrows(ConcertClosedException.class, () ->
			ticketReservationService.requestReservation(requestDto, member.getId())
		);

	}

	@Test
	@DisplayName("concert 시작 시간 전 예매 테스트")
	void concertBeforeStartTimeExceptionTest() {
		// Given
		TicketReservationRequestDto requestDto = TicketReservationRequestDto.builder()
			.seatId(1L)
			.build();
		ReflectionTestUtils.setField(concert, "reservationStartTime", LocalDateTime.now().plusDays(1));

		// 리포지토리 모킹
		when(concertSeatRepository.findById(anyLong()))
			.thenReturn(Optional.of(concertSeat));
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member));

		// When & Then
		assertThrows(ConcertNotStartDateException.class, () ->
			ticketReservationService.requestReservation(requestDto, member.getId())
		);
	}

	@Test
	@DisplayName("concert 끝나는 시간 후 예매 테스트")
	void concertAfterEndTimeExceptionTest() {
		// Given
		TicketReservationRequestDto requestDto = TicketReservationRequestDto.builder()
			.seatId(1L)
			.build();

		// 콘서트 종료 시간을 과거로 설정
		ReflectionTestUtils.setField(concert, "reservationEndTime", LocalDateTime.now().minusDays(1));

		// 리포지토리 모킹
		when(concertSeatRepository.findById(anyLong()))
			.thenReturn(Optional.of(concertSeat));
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member));

		// When & Then
		assertThrows(ConcertEndTimeException.class, () ->
			ticketReservationService.requestReservation(requestDto, member.getId())
		);
	}

	@Test
	@DisplayName("예약 유저와 다른 유저 시도")
	void approveReservationNotMatchUserException() {
		// Given
		Member differentMember = Member.builder()
			.email("different@example.com")
			.name("다른 사용자")
			.build();
		ReflectionTestUtils.setField(differentMember, "id", 2L);
		// 리포지토리 모킹
		when(ticketReservationRepository.findById(anyLong()))
			.thenReturn(Optional.of(ticketReservation));
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(differentMember));
		// When & Then
		assertThrows(MemberNotMatchException.class, () ->
			ticketReservationService.approveReservation(ticketReservation.getId(), differentMember.getId())
		);
	}

	@Test
	@DisplayName("이미 승인된 상태일 때 에러")
	void approveReservationAlreadyApprovedException() {
		// Given
		ReflectionTestUtils.setField(ticketReservation, "status", TicketReservationStatus.APPROVED);
		ReflectionTestUtils.setField(concert, "reservationEndTime", LocalDateTime.now().plusDays(3));

		// 리포지토리 모킹
		when(ticketReservationRepository.findById(anyLong()))
			.thenReturn(Optional.of(ticketReservation));
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member));

		// When & Then
		assertThrows(TicketReservationStatusException.class, () ->
			ticketReservationService.approveReservation(ticketReservation.getId(), member.getId())
		);
	}

	@Test
	@DisplayName("예약 취소 성공")
	void cancelReservationSuccess() {
		// Given
		ReflectionTestUtils.setField(ticketReservation, "status", TicketReservationStatus.APPROVED);
		ReflectionTestUtils.setField(concert, "startTime", LocalDateTime.now().plusDays(2)); // 콘서트 시작 시간을 충분히 미래로 설정

		// 리포지토리 모킹
		when(ticketReservationRepository.findById(anyLong()))
			.thenReturn(Optional.of(ticketReservation));
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member));
		when(ticketReservationRepository.save(any(TicketReservation.class)))
			.thenReturn(ticketReservation);

		// When
		TicketReservationResponseDto responseDto = ticketReservationService
			.cancelReservation(ticketReservation.getId(), member.getId());

		// Then
		assertNotNull(responseDto);
		assertEquals(TicketReservationStatus.CANCELLED, responseDto.getStatus());

	}

	@Test
	@DisplayName("예약 취소 불가능 테스트 ( 이미 취소 상태 )")
	void cancelReservationStatusCancelTest() {
		// Given
		ReflectionTestUtils.setField(ticketReservation, "status", TicketReservationStatus.CANCELLED);
		ReflectionTestUtils.setField(concert, "startTime", LocalDateTime.now().plusDays(2)); // 충분히 미래 시간

		// 리포지토리 모킹
		when(ticketReservationRepository.findById(anyLong()))
			.thenReturn(Optional.of(ticketReservation));
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member));

		// When & Then
		assertThrows(TicketReservationStatusException.class, () ->
			ticketReservationService.cancelReservation(ticketReservation.getId(), member.getId())
		);
	}


	@Test
	@DisplayName("예약 취소 불가능 테스트 (콘서트 시작 시간 하루전일때)")
	void cancelReservationStartTimeAvailableTest() {
		// Given
		ReflectionTestUtils.setField(ticketReservation, "status", TicketReservationStatus.APPROVED);
		ReflectionTestUtils.setField(concert, "startTime", LocalDateTime.now().plusHours(23)); // 콘서트 시작 시간이 24시간 이내

		// 리포지토리 모킹
		when(ticketReservationRepository.findById(anyLong()))
			.thenReturn(Optional.of(ticketReservation));
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong()))
			.thenReturn(Optional.of(member));

		// When & Then
		assertThrows(ConcertCancelTimeException.class, () ->
			ticketReservationService.cancelReservation(ticketReservation.getId(), member.getId())
		);
	}

	@Test
	@DisplayName("member 예약 리스트 조회")
	void getMemberReservationsSuccess() {
		// Given
		when(ticketReservationRepository.findByMemberId(anyLong()))
			.thenReturn(List.of(ticketReservation));

		// When
		List<TicketReservationResponseDto> reservations = ticketReservationService
			.getMemberReservations(member.getId());

		// Then
		assertNotNull(reservations);
		assertFalse(reservations.isEmpty());
		assertEquals(1, reservations.size());

	}
}
