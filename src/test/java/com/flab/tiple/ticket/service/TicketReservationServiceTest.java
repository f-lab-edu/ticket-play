// package com.flab.tiple.ticket.service;
//
// import static org.assertj.core.api.AssertionsForClassTypes.*;
// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyLong;
// import static org.mockito.Mockito.*;
//
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.test.util.ReflectionTestUtils;
//
// import com.flab.tiple.concert.domain.Concert;
// import com.flab.tiple.concert.domain.ConcertSeat;
// import com.flab.tiple.concert.enums.ConcertSeatGrade;
// import com.flab.tiple.concert.enums.ConcertStatus;
// import com.flab.tiple.concert.enums.SeatStatus;
// import com.flab.tiple.concert.exception.ConcertCancelTimeException;
// import com.flab.tiple.concert.exception.ConcertClosedException;
// import com.flab.tiple.concert.exception.ConcertEndTimeException;
// import com.flab.tiple.concert.exception.ConcertNotStartDateException;
// import com.flab.tiple.concert.exception.ConcertSeatNotFoundException;
// import com.flab.tiple.concert.repository.concert.ConcertRepository;
// import com.flab.tiple.concert.repository.concertSeat.ConcertSeatRepository;
// import com.flab.tiple.global.exception.ErrorCode;
// import com.flab.tiple.member.domain.Member;
// import com.flab.tiple.member.exception.MemberNotMatchException;
// import com.flab.tiple.member.repository.MemberRepository;
// import com.flab.tiple.ticket.reservation.domain.TicketReservation;
// import com.flab.tiple.ticket.reservation.dto.request.TicketReservationRequestDto;
// import com.flab.tiple.ticket.reservation.dto.response.TicketReservationInfoResponseDto;
// import com.flab.tiple.ticket.reservation.dto.response.TicketReservationResponseDto;
// import com.flab.tiple.ticket.reservation.dto.response.TicketReservationServiceFindInfo;
// import com.flab.tiple.ticket.reservation.enums.TicketProcessStatus;
// import com.flab.tiple.ticket.reservation.enums.TicketReservationStatus;
// import com.flab.tiple.ticket.reservation.exception.TicketReservationStatusException;
// import com.flab.tiple.ticket.reservation.initializer.StatusTransitionJsonInitializer;
// import com.flab.tiple.ticket.reservation.repository.TicketReservationRepository;
// import com.flab.tiple.ticket.reservation.service.TicketReservationServiceImpl;
// import com.flab.tiple.ticket.waiting.domain.TicketWaiting;
// import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingResponseDto;
// import com.flab.tiple.ticket.waiting.enums.TicketWaitingStatus;
// import com.flab.tiple.ticket.waiting.exception.TicketWaitingRegisterException;
// import com.flab.tiple.ticket.waiting.repository.TicketWaitingRepository;
//
// @ExtendWith(MockitoExtension.class)
// public class TicketReservationServiceTest {
//
// 	@InjectMocks
// 	private TicketReservationServiceImpl ticketReservationService;
//
// 	@Mock
// 	private TicketReservationRepository ticketReservationRepository;
//
// 	@Mock
// 	private ConcertSeatRepository concertSeatRepository;
//
// 	@Mock
// 	private MemberRepository memberRepository;
//
// 	@Mock
// 	private ConcertRepository concertRepository;
//
// 	@Mock
// 	private TicketWaitingRepository ticketWaitingRepository;
//
// 	@InjectMocks
// 	private TicketReservationServiceImpl ticketReservationServiceImpl;
//
// 	private Member member;
// 	private ConcertSeat concertSeat;
// 	private Concert concert;
// 	private TicketWaiting ticketWaiting;
// 	private TicketReservation ticketReservation;
// 	private TicketReservationRequestDto requestDto;
// 	private Long memberId = 1L;
// 	private TicketWaitingResponseDto mockWaitingResponse;
// 	private TicketReservationServiceFindInfo ticketReservationServiceFindInfo;
//
// 	@BeforeEach
// 	void setUp() {
// 		StatusTransitionJsonInitializer.initializeForTest();
// 		// 멤버 생성
// 		member = Member.builder()
// 			.email("test@example.com")
// 			.name("테스트 사용자")
// 			.build();
// 		ReflectionTestUtils.setField(member, "id", 1L);
//
// 		// 콘서트 생성 (현재 시간 기준으로 유효한 콘서트)
// 		concert = Concert.builder()
// 			.name("Shining Star 콘서트")
// 			.artistName("엑소")
// 			.startTime(LocalDateTime.now().plusHours(3))
// 			.endTime(LocalDateTime.now().plusHours(9))
// 			.reservationStartTime(LocalDateTime.now().minusDays(2))
// 			.reservationEndTime(LocalDateTime.now().plusDays(1))
// 			.remainingSeat(100)
// 			.status(ConcertStatus.OPEN)
// 			.concertHallName("서울 콘서트홀")
// 			.concertHallAddress("서울시 강남구")
// 			.concertSeatInfo("일반석, VIP석")
// 			.build();
//
// 		// Reflection을 사용해 ID 설정
// 		ReflectionTestUtils.setField(concert, "id", 1L);
// 		ReflectionTestUtils.setField(concert, "deletedAt", null);
// 		ReflectionTestUtils.setField(concert, "createdAt", LocalDateTime.now());
//
// 		// 좌석 생성
// 		concertSeat = ConcertSeat.builder()
// 			.concert(concert)
// 			.seatNumber(1)
// 			.grade(ConcertSeatGrade.A)
// 			.status(SeatStatus.AVAILABLE)
// 			.build();
// 		ReflectionTestUtils.setField(concertSeat, "id", 1L);
//
// 		// 티켓 예약 생성
// 		ticketReservation = TicketReservation.builder()
// 			.member(member)
// 			.seat(concertSeat)
// 			.status(TicketReservationStatus.PENDING)
// 			.build();
// 		ReflectionTestUtils.setField(ticketReservation, "id", 1L);
// 		ReflectionTestUtils.setField(ticketReservation, "createdAt", LocalDateTime.now());
//
// 		ticketReservationServiceFindInfo = TicketReservationServiceFindInfo.builder()
// 			.member(member)
// 			.concertSeat(concertSeat)
// 			.concert(concert)
// 			.ticketReservation(ticketReservation)
// 			.build();
//
// 		ticketWaiting = TicketWaiting.builder()
// 			.concert(concert)
// 			.member(member)
// 			.status(TicketWaitingStatus.WAITING)
// 			.waitingNumber(1)
// 			.build();
// 		ReflectionTestUtils.setField(ticketWaiting, "id", 1L);
// 		ReflectionTestUtils.setField(ticketWaiting, "createdAt", LocalDateTime.now());
//
// 		requestDto = TicketReservationRequestDto.builder()
// 			.seatId(1L)
// 			.build();
//
// 		mockWaitingResponse = TicketWaitingResponseDto.builder()
// 			.waitingNumber(1)
// 			.concertId(1L)
// 			.concertName("Test Concert")
// 			.status("WAITING")
// 			.build();
//
// 	}
// 	@Nested
// 	@DisplayName("티켓 예약 요청 테스트")
// 	class RequestReservationTest {
//
// 		@Test
// 		@DisplayName("좌석이 Available 상태일 때 예약 성공")
// 		void requestReservationSeatAvailableSuccess() {
// 			when(ticketReservationRepository.save(any(TicketReservation.class))).thenReturn(ticketReservation);
// 			when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
// 			when(concertSeatRepository.findByIdWithPessimisticLock(eq(concertSeat.getId())))
// 				.thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(eq(concert.getId())))
// 				.thenReturn(Optional.of(concert));
//
// 			// When
// 			TicketReservationResponseDto<?> result = ticketReservationService.requestReservation(requestDto, memberId);
//
// 			// Then
// 			assertThat(result).isNotNull();
// 			assertThat(result.getStatus()).isEqualTo(TicketProcessStatus.SUCCESS);
// 		}
//
// 	}
//
// 	@Nested
// 	@DisplayName("좌석 예약 시도 테스트")
// 	class TryReserveSeatTest {
//
// 		@Test
// 		@DisplayName("좌석 정보가 없을 때 예외 발생")
// 		void tryReserveSeatSeatNotFoundThrowsException() {
// 			when(concertSeatRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.empty());
//
// 			assertThrows(ConcertSeatNotFoundException.class, () ->
// 				ticketReservationService.tryReserveSeat(requestDto, memberId)
// 			);
// 		}
//
// 		@Test
// 		@DisplayName("정상적인 좌석 예약 성공")
// 		void tryReserveSeatValidRequestSuccess() {
// 			when(ticketReservationRepository.save(any(TicketReservation.class))).thenReturn(ticketReservation);
// 			when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
// 			when(concertSeatRepository.findByIdWithPessimisticLock(eq(concertSeat.getId())))
// 				.thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(eq(concert.getId())))
// 				.thenReturn(Optional.of(concert));
//
// 			// When
// 			TicketReservationInfoResponseDto result = ticketReservationService.tryReserveSeat(requestDto, memberId);
//
// 			// Then
// 			assertThat(result).isNotNull();
// 			assertThat(result.getId()).isEqualTo(1L);
// 			assertThat(result.getStatus()).isEqualTo(TicketReservationStatus.PENDING);
//
// 			verify(concertSeatRepository).save(concertSeat);
// 			verify(ticketReservationRepository).save(any(TicketReservation.class));
// 		}
// 	}
//
// 	@Nested
// 	@DisplayName("웨이팅 리스트 등록 테스트")
// 	class ProcessWaitingRegistrationTest {
//
// 		@Test
// 		@DisplayName("웨이팅 리스트 등록 성공")
// 		void processWaitingRegistrationSuccess() {
// 			when(concertSeatRepository.findByIdWithPessimisticLock(anyLong())).thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(anyLong())).thenReturn(Optional.of(concert));
// 			when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
// 			when(ticketWaitingRepository.save(any(TicketWaiting.class))).thenReturn(ticketWaiting);
// 			when(ticketReservationRepository.findByIdWithPessimisticLock(anyLong()))
// 				.thenReturn(Optional.of(ticketReservation));
//
// 			ReflectionTestUtils.setField(concert, "remainingSeat", 0);
// 			// When
// 			TicketWaitingResponseDto result = ticketReservationService.processWaitingRegistration(requestDto, memberId);
//
// 			// Then
// 			assertThat(result).isNotNull();
// 			assertThat(result.getWaitingNumber()).isEqualTo(1);
// 			assertThat(result.getConcertId()).isEqualTo(1L);
// 		}
//
// 		@Test
// 		@DisplayName("웨이팅 리스트 등록 중 예외 발생 시 TicketWaitingRegisterException 발생")
// 		void processWaitingRegistrationExceptionThrowsTicketWaitingRegisterException() {
// 			when(concertSeatRepository.findById(1L)).thenThrow(new ConcertSeatNotFoundException(ErrorCode.CONCERT_SEAT_NOT_FOUND, ErrorCode.CONCERT_SEAT_NOT_FOUND.getDescription()));
//
// 			// When & Then
// 			assertThrows(TicketWaitingRegisterException.class, () ->
// 				ticketReservationService.processWaitingRegistration(requestDto, memberId)
// 			);
// 		}
// 	}
//
// 	@Nested
// 	@DisplayName("웨이팅 등록 테스트")
// 	class RegisterWaitingTest {
//
// 		@Test
// 		@DisplayName("웨이팅 번호 할당 및 등록 성공")
// 		void registerWaitingSuccess() {
// 			// Given
// 			when(ticketWaitingRepository.findMaxWaitingNumberByConcertId(1L)).thenReturn(Optional.of(5));
// 			when(ticketWaitingRepository.save(any(TicketWaiting.class))).thenReturn(ticketWaiting);
//
// 			// When
// 			TicketWaitingResponseDto result = ticketReservationService.registerWaiting(ticketReservationServiceFindInfo);
//
// 			// Then
// 			assertThat(result).isNotNull();
// 			assertThat(result.getWaitingNumber()).isEqualTo(6);
// 			assertThat(result.getConcertId()).isEqualTo(1L);
//
// 			verify(ticketWaitingRepository).findMaxWaitingNumberByConcertId(1L);
// 			verify(ticketWaitingRepository).save(any(TicketWaiting.class));
// 		}
//
// 		@Test
// 		@DisplayName("첫 번째 웨이팅 번호 할당 성공")
// 		void registerWaitingFirstWaitingSuccess() {
// 			// Given
// 			when(ticketWaitingRepository.findMaxWaitingNumberByConcertId(1L)).thenReturn(Optional.empty());
// 			when(ticketWaitingRepository.save(any(TicketWaiting.class))).thenReturn(ticketWaiting);
//
// 			// When
// 			TicketWaitingResponseDto result = ticketReservationService.registerWaiting(ticketReservationServiceFindInfo);
//
// 			// Then
// 			assertThat(result).isNotNull();
// 			assertThat(result.getWaitingNumber()).isEqualTo(1);
// 			assertThat(result.getConcertId()).isEqualTo(1L);
// 			verify(ticketWaitingRepository).findMaxWaitingNumberByConcertId(1L);
// 			verify(ticketWaitingRepository).save(any(TicketWaiting.class));
// 		}
// 	}
//
//
// 	@Nested
// 	@DisplayName("티켓 예약시 예외 테스트")
// 	class RequestReservationExceptionTest {
// 		@Test
// 		@DisplayName("concert 상태 관련 예외 테스트")
// 		void concertStatusExceptionCheck() {
// 			// Given
// 			ReflectionTestUtils.setField(concert, "status", ConcertStatus.CLOSED);
//
// 			// 리포지토리 모킹
// 			when(concertSeatRepository.findByIdWithPessimisticLock(eq(concertSeat.getId())))
// 				.thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(eq(concert.getId())))
// 				.thenReturn(Optional.of(concert));
// 			when(memberRepository.findById(anyLong()))
// 				.thenReturn(Optional.of(member));
//
// 			// When & Then
// 			assertThrows(ConcertClosedException.class, () ->
// 				ticketReservationServiceImpl.requestReservation(requestDto, member.getId())
// 			);
//
// 		}
//
// 		@Test
// 		@DisplayName("concert 시작 시간 전 예매 테스트")
// 		void concertBeforeStartTimeExceptionTest() {
// 			// Given
// 			ReflectionTestUtils.setField(concert, "reservationStartTime", LocalDateTime.now().plusDays(1));
//
// 			// 리포지토리 모킹
// 			when(concertSeatRepository.findByIdWithPessimisticLock(eq(concertSeat.getId())))
// 				.thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(eq(concert.getId())))
// 				.thenReturn(Optional.of(concert));
// 			when(memberRepository.findById(anyLong()))
// 				.thenReturn(Optional.of(member));
//
// 			// When & Then
// 			assertThrows(ConcertNotStartDateException.class, () ->
// 				ticketReservationServiceImpl.requestReservation(requestDto, member.getId())
// 			);
// 		}
//
// 		@Test
// 		@DisplayName("concert 끝나는 시간 후 예매 테스트")
// 		void concertAfterEndTimeExceptionTest() {
// 			// Given
// 			// 콘서트 종료 시간을 과거로 설정
// 			ReflectionTestUtils.setField(concert, "reservationEndTime", LocalDateTime.now().minusDays(1));
//
// 			// 리포지토리 모킹
// 			when(concertSeatRepository.findByIdWithPessimisticLock(eq(concertSeat.getId())))
// 				.thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(eq(concert.getId())))
// 				.thenReturn(Optional.of(concert));
// 			when(memberRepository.findById(anyLong()))
// 				.thenReturn(Optional.of(member));
//
// 			// When & Then
// 			assertThrows(ConcertEndTimeException.class, () ->
// 				ticketReservationServiceImpl.requestReservation(requestDto, member.getId())
// 			);
// 		}
//
// 		@Test
// 		@DisplayName("예약 유저와 다른 유저 시도")
// 		void approveReservationNotMatchUserException() {
// 			// Given
// 			Member differentMember = Member.builder()
// 				.email("different@example.com")
// 				.name("다른 사용자")
// 				.build();
// 			ReflectionTestUtils.setField(differentMember, "id", 2L);
// 			// 리포지토리 모킹
// 			when(ticketReservationRepository.findByIdWithPessimisticLock(anyLong()))
// 				.thenReturn(Optional.of(ticketReservation));
// 			when(concertSeatRepository.findByIdWithPessimisticLock(eq(concertSeat.getId())))
// 				.thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(eq(concert.getId())))
// 				.thenReturn(Optional.of(concert));
// 			when(memberRepository.findById(anyLong()))
// 				.thenReturn(Optional.of(differentMember));
// 			// When & Then
// 			assertThrows(MemberNotMatchException.class, () ->
// 				ticketReservationServiceImpl.approveReservation(ticketReservation.getId(), differentMember.getId())
// 			);
// 		}
//
// 		@Test
// 		@DisplayName("이미 승인된 상태일 때 에러")
// 		void approveReservationAlreadyApprovedException() {
// 			// Given
// 			ReflectionTestUtils.setField(ticketReservation, "status", TicketReservationStatus.APPROVED);
// 			ReflectionTestUtils.setField(concert, "reservationEndTime", LocalDateTime.now().plusDays(3));
//
// 			// 리포지토리 모킹
// 			when(ticketReservationRepository.findByIdWithPessimisticLock(anyLong()))
// 				.thenReturn(Optional.of(ticketReservation));
// 			when(concertSeatRepository.findByIdWithPessimisticLock(eq(concertSeat.getId())))
// 				.thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(eq(concert.getId())))
// 				.thenReturn(Optional.of(concert));
// 			when(memberRepository.findById(anyLong()))
// 				.thenReturn(Optional.of(member));
//
// 			// When & Then
// 			assertThrows(TicketReservationStatusException.class, () ->
// 				ticketReservationServiceImpl.approveReservation(ticketReservation.getId(), member.getId())
// 			);
// 		}
// 	}
//
// 	@Nested
// 	@DisplayName("티켓 예약 취소 테스트")
// 	class RequestReservationCancelTest {
// 		@Test
// 		@DisplayName("예약 취소 성공")
// 		void cancelReservationSuccess() {
// 			// Given
// 			ReflectionTestUtils.setField(ticketReservation, "status", TicketReservationStatus.APPROVED);
// 			ReflectionTestUtils.setField(concert, "startTime",
// 				LocalDateTime.now().plusDays(2)); // 콘서트 시작 시간을 충분히 미래로 설정
//
// 			// 리포지토리 모킹
// 			// 리포지토리 모킹 - 순서와 ID 일치 확인
// 			when(ticketReservationRepository.findByIdWithPessimisticLock(anyLong()))
// 				.thenReturn(Optional.of(ticketReservation));
// 			// 좌석 ID를 명시적으로 지정하여 모킹
// 			when(concertSeatRepository.findByIdWithPessimisticLock(eq(concertSeat.getId())))
// 				.thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(eq(concert.getId())))
// 				.thenReturn(Optional.of(concert));
// 			when(memberRepository.findById(anyLong()))
// 				.thenReturn(Optional.of(member));
// 			when(ticketReservationRepository.save(any(TicketReservation.class)))
// 				.thenReturn(ticketReservation);
//
// 			// When
// 			TicketReservationInfoResponseDto responseDto = ticketReservationServiceImpl
// 				.cancelReservation(ticketReservation.getId(), member.getId());
//
// 			// Then
// 			assertNotNull(responseDto);
// 			assertEquals(TicketReservationStatus.CANCELLED, responseDto.getStatus());
//
// 		}
//
// 		@Test
// 		@DisplayName("예약 취소 불가능 테스트 ( 이미 취소 상태 )")
// 		void cancelReservationStatusCancelTest() {
// 			// Given
// 			ReflectionTestUtils.setField(ticketReservation, "status", TicketReservationStatus.CANCELLED);
// 			ReflectionTestUtils.setField(concert, "startTime", LocalDateTime.now().plusDays(2)); // 충분히 미래 시간
//
// 			// 리포지토리 모킹
// 			when(ticketReservationRepository.findByIdWithPessimisticLock(anyLong()))
// 				.thenReturn(Optional.of(ticketReservation));
// 			when(concertSeatRepository.findByIdWithPessimisticLock(eq(concertSeat.getId())))
// 				.thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(eq(concert.getId())))
// 				.thenReturn(Optional.of(concert));
// 			when(memberRepository.findById(anyLong()))
// 				.thenReturn(Optional.of(member));
//
// 			// When & Then
// 			assertThrows(TicketReservationStatusException.class, () ->
// 				ticketReservationServiceImpl.cancelReservation(ticketReservation.getId(), member.getId())
// 			);
// 		}
//
// 		@Test
// 		@DisplayName("예약 취소 불가능 테스트 (콘서트 시작 시간 하루전일때)")
// 		void cancelReservationStartTimeAvailableTest() {
// 			// Given
// 			ReflectionTestUtils.setField(ticketReservation, "status", TicketReservationStatus.APPROVED);
// 			ReflectionTestUtils.setField(concert, "startTime", LocalDateTime.now().plusHours(23)); // 콘서트 시작 시간이 24시간 이내
//
// 			// 리포지토리 모킹
// 			when(ticketReservationRepository.findByIdWithPessimisticLock(anyLong()))
// 				.thenReturn(Optional.of(ticketReservation));
// 			when(concertSeatRepository.findByIdWithPessimisticLock(eq(concertSeat.getId())))
// 				.thenReturn(Optional.of(concertSeat));
// 			when(concertRepository.findByIdWithPessimisticLock(eq(concert.getId())))
// 				.thenReturn(Optional.of(concert));
// 			when(memberRepository.findById(anyLong()))
// 				.thenReturn(Optional.of(member));
//
// 			// When & Then
// 			assertThrows(ConcertCancelTimeException.class, () ->
// 				ticketReservationServiceImpl.cancelReservation(ticketReservation.getId(), member.getId())
// 			);
// 		}
// 	}
//
// 	@Test
// 	@DisplayName("member 예약 리스트 조회")
// 	void getMemberReservationsSuccess() {
// 		// Given
// 		when(ticketReservationRepository.findByMemberId(anyLong()))
// 			.thenReturn(List.of(ticketReservation));
//
// 		// When
// 		List<TicketReservationInfoResponseDto> reservations = ticketReservationServiceImpl
// 			.getMemberReservations(member.getId());
//
// 		// Then
// 		assertNotNull(reservations);
// 		assertFalse(reservations.isEmpty());
// 		assertEquals(1, reservations.size());
//
// 	}
// }
