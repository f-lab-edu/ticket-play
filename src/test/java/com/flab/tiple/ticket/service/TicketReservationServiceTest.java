package com.flab.tiple.ticket.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.concert.domain.ConcertSeat;
import com.flab.tiple.concert.enums.ConcertSeatGrade;
import com.flab.tiple.concert.enums.ConcertStatus;
import com.flab.tiple.concert.enums.SeatStatus;
import com.flab.tiple.concert.repository.concert.ConcertRepository;
import com.flab.tiple.concert.repository.concertSeat.ConcertSeatRepository;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.repository.MemberRepository;
import com.flab.tiple.global.infrastructure.notification.dto.NotificationDto;
import com.flab.tiple.global.infrastructure.notification.sse.SseEmitterService;
import com.flab.tiple.ticket.reservation.domain.model.TicketReservation;
import com.flab.tiple.ticket.reservation.domain.model.TicketReservationRedis;
import com.flab.tiple.ticket.reservation.application.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.reservation.application.dto.response.TicketReservationInfoResponseDto;
import com.flab.tiple.ticket.reservation.application.dto.response.TicketReservationResponseDto;
import com.flab.tiple.ticket.reservation.domain.model.enums.TicketProcessStatus;
import com.flab.tiple.ticket.reservation.domain.model.enums.TicketReservationStatus;
import com.flab.tiple.ticket.reservation.domain.exception.TicketReservationNotFoundException;
import com.flab.tiple.ticket.reservation.domain.repository.TicketReservationRedisRepository;
import com.flab.tiple.ticket.reservation.domain.repository.TicketReservationRepository;
import com.flab.tiple.ticket.reservation.domain.service.TicketReservationServiceImpl;
import com.flab.tiple.ticket.waiting.domain.TicketWaitingRedis;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingResponseDto;
import com.flab.tiple.ticket.waiting.enums.TicketWaitingStatus;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRedisRepository;

import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@MockitoSettings(strictness = Strictness.LENIENT) // 불필요한 스텁 경고 해제
public class TicketReservationServiceTest {

	@Mock
	private TicketReservationRepository ticketReservationRepository;

	@Mock
	private ConcertSeatRepository concertSeatRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private ConcertRepository concertRepository;

	@Mock
	private TicketWaitingRedisRepository ticketWaitingRedisRepository;

	@Mock
	private TicketReservationRedisRepository ticketReservationRedisRepository;

	@Mock
	private SseEmitterService sseEmitterService;

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private TicketReservationServiceImpl ticketReservationService;

	private Member member;
	private Concert concert;
	private ConcertSeat concertSeat;
	private TicketReservation ticketReservation;
	private TicketReservationRequestDto requestDto;

	@BeforeEach
	void setUp() {
		// StringRedisTemplate 설정은 필요한 테스트에서만 사용

		// 테스트용 기본 데이터 설정
		member = Member.builder()
			.email("test@example.com")
			.name("테스트 사용자")
			.build();
		ReflectionTestUtils.setField(member, "id", 1L);

		// 콘서트 시작 시간을 더 멀리 설정 (취소 시간 검증 통과를 위해)
		concert = Concert.builder()
			.name("Shining Star 콘서트")
			.artistName("엑소")
			.startTime(LocalDateTime.now().plusDays(7)) // 7일 후로 설정
			.endTime(LocalDateTime.now().plusDays(7).plusHours(3))
			.reservationStartTime(LocalDateTime.now().minusDays(2))
			.reservationEndTime(LocalDateTime.now().plusDays(1))
			.remainingSeat(100)
			.status(ConcertStatus.OPEN)
			.concertHallName("서울 콘서트홀")
			.concertHallAddress("서울시 강남구")
			.concertSeatInfo("일반석, VIP석")
			.build();

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

		requestDto = TicketReservationRequestDto.builder()
			.seatId(1L)
			.concertId(1L)
			.build();
	}

	@Test
	@DisplayName("티켓 예약 요청 - 성공 케이스")
	void requestReservation_Success() {
		// Given
		when(concertSeatRepository.findById(anyLong())).thenReturn(Optional.of(concertSeat));
		when(concertRepository.findById(anyLong())).thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
		when(ticketReservationRepository.save(any(TicketReservation.class))).thenReturn(ticketReservation);

		// When
		TicketReservationResponseDto<?> result = ticketReservationService.requestReservation(requestDto, 1L);

		// Then
		assertThat(result.getStatus()).isEqualTo(TicketProcessStatus.SUCCESS);
		assertThat(result.getData()).isInstanceOf(TicketReservationInfoResponseDto.class);

		// 필수 메서드 호출 검증
		verify(concertSeatRepository).findById(eq(1L));
		verify(concertRepository).findById(eq(1L));
		verify(memberRepository).findById(eq(1L));
		verify(ticketReservationRepository).save(any(TicketReservation.class));
	}

	@Test
	@DisplayName("티켓 예약 요청 - 좌석이 이미 예약된 경우 웨이팅 등록")
	void requestReservation_SeatAlreadyReserved_WaitingRegistration() {
		// Given
		// Redis ValueOperations 설정
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.increment(anyString())).thenReturn(5L); // 대기 번호 5 할당

		// 좌석 상태 설정
		concertSeat.pending(); // 좌석 상태를 PENDING으로 변경
		ReflectionTestUtils.setField(concert, "remainingSeat", 0);

		// 모킹 설정
		when(concertSeatRepository.findById(anyLong())).thenReturn(Optional.of(concertSeat));
		when(concertRepository.findById(anyLong())).thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
		when(ticketWaitingRedisRepository.findByConcertIdAndMemberId(anyLong(), anyLong()))
			.thenReturn(Optional.empty());

		// When
		TicketReservationResponseDto<?> result = ticketReservationService.requestReservation(requestDto, 1L);

		// Then
		assertThat(result.getStatus()).isEqualTo(TicketProcessStatus.WAITING);
		assertThat(result.getData()).isInstanceOf(TicketWaitingResponseDto.class);
		TicketWaitingResponseDto waitingDto = (TicketWaitingResponseDto) result.getData();
		assertThat(waitingDto.getWaitingNumber()).isEqualTo(5);

		// 필수 메서드 호출 검증
		verify(ticketWaitingRedisRepository).findByConcertIdAndMemberId(eq(1L), eq(1L));
		verify(valueOperations).increment(anyString());
	}

	@Test
	@DisplayName("티켓 예약 승인 - 성공 케이스")
	void approveReservation_Success() throws Exception {
		// Given
		// 테스트용 모킹 객체 생성
		TicketReservation mockReservation = Mockito.mock(TicketReservation.class);
		when(mockReservation.getId()).thenReturn(1L);
		when(mockReservation.getMember()).thenReturn(member);
		when(mockReservation.getSeat()).thenReturn(concertSeat);
		when(mockReservation.getStatus()).thenReturn(TicketReservationStatus.APPROVED);
		when(mockReservation.getCreatedAt()).thenReturn(LocalDateTime.now());

		// 필요한 메서드 동작 설정
		when(ticketReservationRepository.findById(anyLong())).thenReturn(Optional.of(mockReservation));
		when(concertRepository.findById(anyLong())).thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));

		// Redis에 타이머 정보 설정
		String redisKey = concertSeat.getId() + ":" + member.getId();
		TicketReservationRedis reservationRedis = new TicketReservationRedis(concertSeat.getId(), member.getId(), concert.getId());
		when(ticketReservationRedisRepository.findById(redisKey)).thenReturn(Optional.of(reservationRedis));

		when(ticketReservationRepository.save(any(TicketReservation.class))).thenReturn(mockReservation);

		// When
		TicketReservationInfoResponseDto result = ticketReservationService.approveReservation(1L, 1L);

		// Then
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getStatus()).isEqualTo(TicketReservationStatus.APPROVED);

		// 필수 메서드 호출 검증
		verify(ticketReservationRedisRepository).findById(eq(redisKey));
		verify(ticketReservationRedisRepository).deleteById(eq(redisKey));
	}

	@Test
	@DisplayName("티켓 예약 승인 - Redis 타이머 만료된 경우")
	void approveReservation_RedisTimerExpired() throws Exception {
		// Given
		// 테스트용 모킹 객체 생성
		TicketReservation mockReservation = Mockito.mock(TicketReservation.class);
		when(mockReservation.getId()).thenReturn(1L);
		when(mockReservation.getMember()).thenReturn(member);
		when(mockReservation.getSeat()).thenReturn(concertSeat);
		when(mockReservation.getStatus()).thenReturn(TicketReservationStatus.PENDING);

		// 필요한 메서드 동작 설정
		when(ticketReservationRepository.findById(anyLong())).thenReturn(Optional.of(mockReservation));
		when(concertRepository.findById(anyLong())).thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));

		// Redis에 타이머 정보 없음 (타임아웃)
		String redisKey = concertSeat.getId() + ":" + member.getId();
		when(ticketReservationRedisRepository.findById(redisKey)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> ticketReservationService.approveReservation(1L, 1L))
			.isInstanceOf(TicketReservationNotFoundException.class)
			.hasMessageContaining(ErrorCode.TICKET_RESERVATION_TIMEOUT.getDescription());
	}

	@Test
	@DisplayName("티켓 예약 취소 - 성공 케이스")
	void cancelReservation_Success() throws Exception {
		// Given
		// 테스트용 모킹 객체 생성
		TicketReservation mockReservation = Mockito.mock(TicketReservation.class);
		when(mockReservation.getId()).thenReturn(1L);
		when(mockReservation.getMember()).thenReturn(member);
		when(mockReservation.getSeat()).thenReturn(concertSeat);
		when(mockReservation.getStatus()).thenReturn(TicketReservationStatus.CANCELLED);
		when(mockReservation.getCreatedAt()).thenReturn(LocalDateTime.now());

		// 필요한 메서드 동작 설정
		when(ticketReservationRepository.findById(anyLong())).thenReturn(Optional.of(mockReservation));
		when(concertRepository.findById(anyLong())).thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
		when(ticketReservationRepository.save(any(TicketReservation.class))).thenReturn(mockReservation);

		// 대기자 없음 설정
		when(ticketWaitingRedisRepository.findByConcertIdOrderByWaitingNumberAsc(anyLong()))
			.thenReturn(List.of());

		// When
		TicketReservationInfoResponseDto result = ticketReservationService.cancelReservation(1L, 1L);

		// Then
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getStatus()).isEqualTo(TicketReservationStatus.CANCELLED);

		// 필수 메서드 호출 검증
		verify(ticketWaitingRedisRepository).findByConcertIdOrderByWaitingNumberAsc(eq(1L));
	}

	@Test
	@DisplayName("티켓 예약 취소 - 대기자 존재하는 경우 알림 처리")
	void cancelReservation_WithWaitingUsers() throws Exception {
		// Given
		// 테스트용 모킹 객체 생성
		TicketReservation mockReservation = Mockito.mock(TicketReservation.class);
		when(mockReservation.getId()).thenReturn(1L);
		when(mockReservation.getMember()).thenReturn(member);
		when(mockReservation.getSeat()).thenReturn(concertSeat);
		when(mockReservation.getStatus()).thenReturn(TicketReservationStatus.CANCELLED);
		when(mockReservation.getCreatedAt()).thenReturn(LocalDateTime.now());

		// 필요한 메서드 동작 설정
		when(ticketReservationRepository.findById(anyLong())).thenReturn(Optional.of(mockReservation));
		when(concertRepository.findById(anyLong())).thenReturn(Optional.of(concert));
		when(memberRepository.findById(anyLong())).thenReturn(Optional.of(member));
		when(ticketReservationRepository.save(any(TicketReservation.class))).thenReturn(mockReservation);

		// 대기자 존재 시나리오
		TicketWaitingRedis waitingRedis = TicketWaitingRedis.builder()
			.memberId(2L)
			.concertId(1L)
			.waitingNumber(1)
			.status(TicketWaitingStatus.WAITING.toString())
			.build();

		when(ticketWaitingRedisRepository.findByConcertIdOrderByWaitingNumberAsc(anyLong()))
			.thenReturn(List.of(waitingRedis));

		doNothing().when(sseEmitterService).sendToMember(anyLong(), any(), anyString());

		// When
		TicketReservationInfoResponseDto result = ticketReservationService.cancelReservation(1L, 1L);

		// Then
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getStatus()).isEqualTo(TicketReservationStatus.CANCELLED);

		// 알림 처리 메서드 호출 확인
		verify(sseEmitterService).sendToMember(eq(2L), any(NotificationDto.class), anyString());
	}

	@Test
	@DisplayName("회원의 티켓 예약 목록 조회")
	void getMemberReservations() {
		// Given
		when(ticketReservationRepository.findByMemberId(anyLong())).thenReturn(List.of(ticketReservation));

		// When
		List<TicketReservationInfoResponseDto> results = ticketReservationService.getMemberReservations(1L);

		// Then
		assertThat(results).hasSize(1);
		assertThat(results.get(0).getId()).isEqualTo(1L);

		// 메서드 호출 확인
		verify(ticketReservationRepository).findByMemberId(eq(1L));
	}

	@Test
	@DisplayName("예약 타임아웃 처리")
	void handleReservationTimeout() {
		// Given
		when(ticketReservationRepository.findBySeatIdAndMemberId(anyLong(), anyLong()))
			.thenReturn(Optional.of(ticketReservation));

		// Concert 조회 결과 모킹
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));

		when(ticketWaitingRedisRepository.findByConcertIdOrderByWaitingNumberAsc(anyLong()))
			.thenReturn(List.of());

		// When
		ticketReservationService.handleReservationTimeout(1L, 1L);

		// Then
		// 필수 메서드 호출 검증만 수행
		verify(ticketReservationRepository).save(any(TicketReservation.class));
		verify(concertSeatRepository).save(any(ConcertSeat.class));
		verify(concertRepository).save(any(Concert.class));
	}

	@Test
	@DisplayName("만료된 예약 일괄 처리")
	void handleExpiredReservations() {
		// Given
		LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
		when(ticketReservationRepository.findByStatusAndCreatedAtBefore(
			eq(TicketReservationStatus.PENDING), any(LocalDateTime.class)))
			.thenReturn(List.of(ticketReservation));

		when(ticketReservationRepository.findBySeatIdAndMemberId(anyLong(), anyLong()))
			.thenReturn(Optional.of(ticketReservation));

		// Concert 조회 결과 모킹
		when(concertRepository.findById(anyLong()))
			.thenReturn(Optional.of(concert));

		when(ticketWaitingRedisRepository.findByConcertIdOrderByWaitingNumberAsc(anyLong()))
			.thenReturn(List.of());

		// When
		ticketReservationService.handleExpiredReservations();

		// Then
		verify(ticketReservationRepository).findByStatusAndCreatedAtBefore(
			eq(TicketReservationStatus.PENDING), any(LocalDateTime.class));

		// 만료된 예약 처리 메서드 호출 확인
		verify(ticketReservationRepository).findBySeatIdAndMemberId(eq(1L), eq(1L));

		// 저장 횟수는 실제 코드 구현에 맞게 정확히 검증 - 1회로 수정
		verify(ticketReservationRepository, times(1)).save(any(TicketReservation.class));
	}
}
