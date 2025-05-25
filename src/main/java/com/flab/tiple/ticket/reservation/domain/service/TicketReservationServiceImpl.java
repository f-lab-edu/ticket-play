package com.flab.tiple.ticket.reservation.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.concert.domain.ConcertSeat;
import com.flab.tiple.concert.dto.response.ConcertSeatInfo;
import com.flab.tiple.concert.exception.ConcertNotFoundException;
import com.flab.tiple.concert.exception.ConcertRemainSeatExistException;
import com.flab.tiple.concert.exception.ConcertSeatNotFoundException;
import com.flab.tiple.concert.exception.ConcertSeatReservationException;
import com.flab.tiple.concert.repository.concert.ConcertRepository;
import com.flab.tiple.concert.repository.concertSeat.ConcertSeatRepository;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.message.TipleMessage;
import com.flab.tiple.global.message.TipleRedisKey;
import com.flab.tiple.global.infrastructure.notification.sse.SseEmitterService;
import com.flab.tiple.global.infrastructure.notification.dto.NotificationDto;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.dto.response.MemberInfoDto;
import com.flab.tiple.member.exception.MemberNotFoundException;
import com.flab.tiple.member.repository.MemberRepository;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationApprovedEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationCanceledEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationCreatedEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.ReservationTimeoutEvent;
import com.flab.tiple.ticket.reservation.application.event.reservation.WaitingRegisteredEvent;
import com.flab.tiple.ticket.reservation.domain.model.TicketReservation;
import com.flab.tiple.ticket.reservation.domain.model.TicketReservationRedis;
import com.flab.tiple.ticket.reservation.application.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.reservation.application.dto.response.TicketReservationInfoResponseDto;
import com.flab.tiple.ticket.reservation.application.dto.response.TicketReservationResponseDto;
import com.flab.tiple.ticket.reservation.application.dto.response.TicketReservationServiceFindInfo;
import com.flab.tiple.ticket.reservation.domain.model.enums.TicketProcessStatus;
import com.flab.tiple.ticket.reservation.domain.model.enums.TicketReservationStatus;
import com.flab.tiple.ticket.reservation.domain.exception.TicketReservationNotFoundException;
import com.flab.tiple.ticket.reservation.domain.repository.TicketReservationRedisRepository;
import com.flab.tiple.ticket.reservation.domain.repository.TicketReservationRepository;
import com.flab.tiple.ticket.waiting.domain.TicketWaiting;
import com.flab.tiple.ticket.waiting.domain.TicketWaitingRedis;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingResponseDto;
import com.flab.tiple.ticket.waiting.enums.TicketWaitingStatus;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingNotFoundException;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingRegisterException;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRedisRepository;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TicketReservationServiceImpl implements TicketReservationService {
	private final TicketReservationRepository ticketReservationRepository;
	private final ConcertSeatRepository concertSeatRepository;
	private final MemberRepository memberRepository;
	private final ConcertRepository concertRepository;
	private final TicketWaitingRepository ticketWaitingRepository;
	private final TicketWaitingRedisRepository ticketWaitingRedisRepository;
	private final TicketReservationRedisRepository ticketReservationRedisRepository;
	private final SseEmitterService sseEmitterService;
	private final RedisTemplate<String, Object> redisTemplate;
	private final StringRedisTemplate stringRedisTemplate; // 추가

	/**
	 * ApplicationEventPublisher란?
	 *
	 * 1. 개념:
	 *    - Spring Framework의 이벤트 발행 메커니즘
	 *    - Observer 패턴의 Spring 구현체
	 *
	 * 2. 장점:
	 *    ─ 낮은 결합도: 이벤트 발행자가 구독자를 직접 알 필요 없음
	 *    ─ 확장성: 새로운 이벤트 리스너 쉽게 추가 가능
	 *    ─ 단일 책임: 각 컴포넌트가 자신의 역할에만 집중
	 *    ─ 테스트 용이성: 이벤트 단위로 독립적 테스트 가능
	 *
	 * 3. Spring Context에서의 역할:
	 *    - ApplicationContext가 ApplicationEventPublisher를 구현
	 *    - 모든 Spring Bean에서 이벤트 발행 가능
	 *    - Spring Boot에서 자동으로 설정됨
	 *
	 *    Spring ApplicationEventPublisher 내부 동작 순서:
	 *    1단계: 이벤트 발행(ApplicationContext.publishEvent() 호출 및 실행)
	 *    2단계: 리스너 검색(ApplicationEventMulticaster.multicastEvent() 호출 -> Spring Context에서 모든 @EventListener 메소드 검색 )
	 *    3단계: 리스너 실행(매칭 리스너가  @Async 없음: 동기 실행 -> 발행자와 같은 스레드 / @Async 있음: 비동기 실행 -> 별도 스레드에서 실행 )
	 */
	// 이벤트 발행을 위한 새로운 의존성 추가
	private final ApplicationEventPublisher eventPublisher;

	private final int REDIS_KEY_TTL = 600;

	/**
	 * 예약 요청 처리 흐름
	 *   1. tryReserveSeat() 시도
	 *   ├─ 좌석 정보 조회
	 *   ├─ 예약 가능성 검증
	 *   └─ DB에 PENDING 예약 생성
	 * ─ 성공 시:
	 *   ├─ ReservationCreated
	 *   │  Event 발행
	 *   └─ SUCCESS 응답
	 * ─ 실패 시:
	 *   ├─ ConcertSeatReservationException 발생
	 *   └─ 대기 등록 로직으로 processWaitingRegistration 이동
	 */
	// 티켓 예약 요청예
	@Override
	public TicketReservationResponseDto<?> requestReservation(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		try {
			// 일반 예약 시도
			TicketReservationInfoResponseDto reservation = tryReserveSeat(requestDto, memberId);

			// 이벤트 발행 - 예약 정보 찾기
			TicketReservation ticketReservation = ticketReservationRepository
				.findById(reservation.getId())
				.orElseThrow(() -> new TicketReservationNotFoundException(
					ErrorCode.TICKET_RESERVATION_NOT_FOUND,
					ErrorCode.TICKET_RESERVATION_NOT_FOUND.getDescription()
				));

			// 예약 생성 이벤트 발행
			eventPublisher.publishEvent(ReservationCreatedEvent.from(ticketReservation));

			return TicketReservationResponseDto.builder()
				.data(reservation)
				.status(TicketProcessStatus.SUCCESS)
				.build();
		} catch (ConcertSeatReservationException e) {
			// 좌석이 사용 불가능한 경우, 웨이팅 처리 로직으로 진행
			TicketWaitingResponseDto waitingReservation = processWaitingRegistration(requestDto, memberId);
			return TicketReservationResponseDto.builder()
				.data(waitingReservation)
				.status(TicketProcessStatus.WAITING)
				.build();
		}
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public TicketReservationInfoResponseDto tryReserveSeat(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		// 1. 정보 찾기
		TicketReservationServiceFindInfo info = findSeatInfo(requestDto.getSeatId(), memberId);
		// 2. 정보 검증
		validateSeatReservation(info);
		// DB에 예약 처리
		return processSeatReservation(info);
	}

	/**
	 * 대기 등록 처리
	 *  ─ 좌석 정보 재조회
	 *  ─ 대기 등록 가능성 검증
	 *
	 *  registerWaitingToRedis():
	 *  ─ 기존 대기 여부 확인
	 *  ─ 대기번호 생성 및 저장
	 *   ─ Redis Counter 사용
	 *   ─ TicketWaitingRedis
	 *    │  객체 생성 및 저장
	 *    └─ 번호 반환
	 *   ─ WaitingRegistered
	 *     Event 발행 -> WAITING 응답
	 */
	@Transactional
	public TicketWaitingResponseDto processWaitingRegistration(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		try {
			// 1. 정보 찾기
			TicketReservationServiceFindInfo info = findSeatInfo(requestDto.getSeatId(), memberId);
			// 2. 정보 검증
			validateWaitingRegistration(info);

			// 3. Redis 기반 대기 등록 처리
			return registerWaitingToRedis(info);

		} catch (ConcertRemainSeatExistException e) {
			throw new ConcertRemainSeatExistException(
				ErrorCode.CONCERT_REMAINING_SEAT_EXIST,
				ErrorCode.CONCERT_REMAINING_SEAT_EXIST.getDescription()
			);
		} catch (Exception ex) {
			throw new TicketWaitingRegisterException(
				ErrorCode.TICKET_WAITING_ERROR,
				ErrorCode.TICKET_WAITING_ERROR.getDescription()
			);
		}
	}


	// Redis 기반 웨이팅 등록 수정
	private TicketWaitingResponseDto registerWaitingToRedis(TicketReservationServiceFindInfo info) {
		Long concertId = info.getConcert().getId();
		Long memberId = info.getMember().getId();

		// 이미 대기 중인지 확인
		Optional<TicketWaitingRedis> existingWaiting =
			ticketWaitingRedisRepository.findByConcertIdAndMemberId(concertId, memberId);

		if (existingWaiting.isPresent()) {
			return buildWaitingResponse(info, existingWaiting.get().getWaitingNumber());
		}

		// 대기 번호 생성 및 Redis 저장
		int waitingNumber = generateAndSaveWaitingNumber(info);

		// 대기 등록 이벤트 발행
		TicketWaitingRedis waitingRedis = ticketWaitingRedisRepository
			.findByConcertIdAndMemberId(concertId, memberId)
			.orElseThrow(() -> new TicketWaitingNotFoundException(
				ErrorCode.TICKET_WAITING_NOT_FOUND,
				ErrorCode.TICKET_WAITING_NOT_FOUND.getDescription()
			));

		eventPublisher.publishEvent(WaitingRegisteredEvent.from(waitingRedis));

		// 응답 생성
		return buildWaitingResponse(info, waitingNumber);
	}

	private TicketWaitingResponseDto buildWaitingResponse(TicketReservationServiceFindInfo info, int waitingNumber) {
		return TicketWaitingResponseDto.builder()
			.concertId(info.getConcert().getId())
			.concertName(info.getConcert().getName())
			.waitingNumber(waitingNumber)
			.status(TicketWaitingStatus.WAITING.toString())
			.build();
	}


	private int generateAndSaveWaitingNumber(TicketReservationServiceFindInfo info) {
		Long concertId = info.getConcert().getId();
		Long memberId = info.getMember().getId();

		String waitingCounterKey = TipleRedisKey.TICKET_WAITING_COUNTER_KEY.getKey() + concertId;
		Long waitingNumber = stringRedisTemplate.opsForValue().increment(waitingCounterKey);

		TicketWaitingRedis waitingRedis = TicketWaitingRedis.builder()
			.concertId(concertId)
			.memberId(memberId)
			.waitingNumber(waitingNumber.intValue())
			.status(TicketWaitingStatus.WAITING)
			.build();

		ticketWaitingRedisRepository.save(waitingRedis);

		return waitingNumber.intValue();
	}


	// 예약 승인
	@Transactional
	@Override
	public TicketReservationInfoResponseDto approveReservation(Long reservationId, Long memberId) {
		// 1. 정보 찾기
		TicketReservationServiceFindInfo info = findReservationInfo(reservationId, memberId);
		// 2. 정보 검증
		validateReservationApproval(info);

		// Redis에서 타이머 정보 확인
		String redisKey = info.getConcertSeat().getId() + ":" + memberId;
		Optional<TicketReservationRedis> reservationRedis = ticketReservationRedisRepository.findById(redisKey);

		if (reservationRedis.isEmpty()) {
			throw new TicketReservationNotFoundException(
				ErrorCode.TICKET_RESERVATION_TIMEOUT,
				ErrorCode.TICKET_RESERVATION_TIMEOUT.getDescription()
			);
		}

		// 3. 정보 수정
		TicketReservationInfoResponseDto result = processReservationApproval(info);

		// 예약 승인 이벤트 발행
		eventPublisher.publishEvent(ReservationApprovedEvent.from(info.getTicketReservation()));

		return result;
	}



	// 예약 취소
	@Transactional
	@Override
	public TicketReservationInfoResponseDto cancelReservation(Long reservationId, Long memberId) {
		// 1. 정보 찾기
		TicketReservationServiceFindInfo info = findReservationInfo(reservationId, memberId);
		// 2. 정보 검증
		validateReservationCancellation(info);

		// 3. 정보 수정
		TicketReservationInfoResponseDto result = processReservationCancellation(info);

		// 예약 취소 이벤트 발행
		eventPublisher.publishEvent(ReservationCanceledEvent.from(info.getTicketReservation()));

		return result;
	}

	private void processNextWaitingUser(Long concertId, Long seatId) {
		try {
			// 1, 대기자 목록 조회
			List<TicketWaitingRedis> waitingList =
				ticketWaitingRedisRepository.findByConcertIdOrderByWaitingNumberAsc(concertId);

			if (waitingList.isEmpty()) {
				return;
			}

			// 2. 첫 번째 대기자에게 알림
			TicketWaitingRedis nextWaiting = waitingList.get(0);

			NotificationDto notification = NotificationDto.builder()
				.message(TipleMessage.RESERVATION_CONCERT_AVAILE_MESSAGE.getMessage())
				.seatId(seatId)
				.concertId(concertId)
				.build();

			sseEmitterService.sendToMember(nextWaiting.getMemberId(), notification, TipleMessage.RESERVATION_SSE_EVENT_NAME.getMessage());
		} catch (Exception e) {
			log.error("대기자 처리 중 예외 발생: {}", e.getMessage(), e);
		}
	}
	// Redis에 예약 정보 저장 (10분 타이머)
	private void saveReservationToRedis(Long seatId, Long memberId, Long concertId) {
		TicketReservationRedis reservationRedis = TicketReservationRedis.builder()
			.seatId(seatId)
			.memberId(memberId)
			.concertId(concertId)
			.build();

		ticketReservationRedisRepository.save(reservationRedis);

		// 만료 시 자동 처리를 위한 리스너 설정
		redisTemplate.expire(
			TipleRedisKey.TICKET_RESERVATION_KEY.getKey()+ reservationRedis.getId(),
			REDIS_KEY_TTL,
			TimeUnit.SECONDS
		);
	}

	@Override
	public List<TicketReservationInfoResponseDto> getMemberReservations(Long memberId) {
		List<TicketReservation> ticketReservations =  ticketReservationRepository.findByMemberId(memberId);
		return ticketReservations.stream().map(this::TicketReservationToDto).toList();
	}

	/**
	 * 타임아웃 처리
	 *  ─ PENDING 예약 조회
	 *   ─ 예약 상태 → TIMEOUT
	 *   ─ 좌석 상태 → AVAILABLE
	 *   ─ 콘서트 잔여석 증가 -> DB 변경사항 저장 -> ReservationTimeout ->Event 발행            │
	 *
	 * + @Scheduled 배치 처리:
	 *  ├─ 10분 경과 PENDING
	 *  │  예약들 일괄 조회
	 *  └─ 각각 타임아웃 처리
	 */
	@Transactional
	@Override
	public void handleReservationTimeout(Long seatId, Long memberId) {
		try {
			TicketReservation reservation = findPendingReservation(seatId, memberId);
			if (reservation == null) return;

			markReservationAsTimeout(reservation);
			releaseSeat(reservation.getSeat());
			increaseRemainingSeat(reservation.getSeat().getConcert().getId());

			// 예약 타임아웃 이벤트 발행
			eventPublisher.publishEvent(ReservationTimeoutEvent.from(reservation));

		} catch (Exception e) {
			log.error("예약 타임아웃 처리 중 오류 발생: {}", e.getMessage(), e);
		}
	}

	private void markReservationAsTimeout(TicketReservation reservation) {
		reservation.timeout(); // 상태 변경
		ticketReservationRepository.save(reservation);
	}

	private void releaseSeat(ConcertSeat seat) {
		seat.cancel(); // 예약 좌석 상태 복구
		concertSeatRepository.save(seat);
	}

	private void increaseRemainingSeat(Long concertId) {
		Concert concert = concertRepository.findById(concertId)
			.orElseThrow(() -> new IllegalArgumentException("Concert not found"));
		concert.cancelSeat(); // 잔여석 증가
		concertRepository.save(concert);
	}

	private TicketReservation findPendingReservation(Long seatId, Long memberId) {
		return ticketReservationRepository
			.findBySeatIdAndMemberId(seatId, memberId)
			.filter(reservation -> reservation.getStatus().equals(TicketReservationStatus.PENDING))
			.orElse(null);
	}


	private TicketWaitingResponseDto TicketWaitingResponseToDto(TicketWaiting ticketWaiting, Integer waitingNumber) {
		Concert ticketConcert = ticketWaiting.getConcert();
		return TicketWaitingResponseDto.builder()
			.concertId(ticketConcert.getId())
			.concertName(ticketConcert.getName())
			.waitingNumber(waitingNumber)
			.status(ticketWaiting.getStatus().name())
			.build();
	}

	private TicketReservationInfoResponseDto TicketReservationToDto(TicketReservation ticketReservation) {
		return TicketReservationInfoResponseDto
			.builder()
			.id(ticketReservation.getId())
			.memberInfo(MemberInfoDto
				.builder()
				.email(ticketReservation.getMember().getEmail())
				.name(ticketReservation.getMember().getName())
				.build())
			.seatInfo(ConcertSeatInfo
				.builder()
				.id(ticketReservation.getSeat().getId())
				.seatNumber(ticketReservation.getSeat().getSeatNumber())
				.grade(ticketReservation.getSeat().getGrade())
				.build()
			)
			.status(ticketReservation.getStatus())
			.createdAt(ticketReservation.getCreatedAt().toString())
			.build();
	}



	// 예약 ID로 정보 조회
	private TicketReservationServiceFindInfo findReservationInfo(Long reservationId, Long memberId) {
		TicketReservation reservation = ticketReservationRepository.findById(reservationId)
			.orElseThrow(() -> new TicketReservationNotFoundException(ErrorCode.TICKET_RESERVATION_NOT_FOUND,
				ErrorCode.TICKET_RESERVATION_NOT_FOUND.getDescription()));

		ConcertSeat concertSeat = reservation.getSeat();

		Concert concert = concertRepository.findById(concertSeat.getConcert().getId())
			.orElseThrow(() -> new ConcertNotFoundException(ErrorCode.CONCERT_NOT_FOUND,
				ErrorCode.CONCERT_NOT_FOUND.getDescription()));

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND,
				ErrorCode.MEMBER_NOT_FOUND.getDescription()));

		return TicketReservationServiceFindInfo.builder()
			.ticketReservation(reservation)
			.concertSeat(concertSeat)
			.concert(concert)
			.member(member)
			.build();
	}

	@Scheduled(fixedDelay = 60000) // 1분마다 실행
	public void handleExpiredReservations() {
		log.info("만료된 예약 처리 스케줄러 실행");
		try {
			// 10분 이상 경과된 PENDING 상태 예약 조회
			List<TicketReservation> pendingReservations =
				ticketReservationRepository.findByStatusAndCreatedAtBefore(
					TicketReservationStatus.PENDING,
					LocalDateTime.now().minusMinutes(10)
				);

			for (TicketReservation reservation : pendingReservations) {
				handleReservationTimeout(reservation.getSeat().getId(), reservation.getMember().getId());
			}
		} catch (Exception e) {
			log.error("만료된 예약 일괄 처리 중 오류 발생: {}", e.getMessage(), e);
		}
	}


	// 좌석 ID로 정보 조회
	private TicketReservationServiceFindInfo findSeatInfo(Long seatId, Long memberId) {
		ConcertSeat seat = concertSeatRepository.findById(seatId)
			.orElseThrow(() -> new ConcertSeatNotFoundException(ErrorCode.CONCERT_SEAT_NOT_FOUND,
				ErrorCode.CONCERT_SEAT_NOT_FOUND.getDescription()));

		Concert concert = concertRepository.findById(seat.getConcert().getId())
			.orElseThrow(() -> new ConcertNotFoundException(ErrorCode.CONCERT_NOT_FOUND,
				ErrorCode.CONCERT_NOT_FOUND.getDescription()));

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND,
				ErrorCode.MEMBER_NOT_FOUND.getDescription()));

		return TicketReservationServiceFindInfo.builder()
			.concertSeat(seat)
			.concert(concert)
			.member(member)
			.build();
	}

	// 예약 승인 검증
	private void validateReservationApproval(TicketReservationServiceFindInfo info) {
		info.getConcert().reservationStatusCheck();
		info.getTicketReservation().checkMatchMember(info.getMember());
		info.getTicketReservation().checkStatus();
	}

	// 예약 취소 검증
	private void validateReservationCancellation(TicketReservationServiceFindInfo info) {
		info.getConcert().reservationStatusCheck();
		info.getConcert().cancelTimeAvailableCheck();
		info.getTicketReservation().checkMatchMember(info.getMember());
		info.getTicketReservation().checkCancelPossibleStatus();
	}

	// 좌석 예약 검증
	private void validateSeatReservation(TicketReservationServiceFindInfo info) {
		info.getConcert().reservationStatusCheck();
	}

	// 웨이팅 등록 검증
	private void validateWaitingRegistration(TicketReservationServiceFindInfo info) {
		info.getConcert().reserveSeatCheck();
	}

	// 예약 승인 처리
	private TicketReservationInfoResponseDto processReservationApproval(TicketReservationServiceFindInfo info) {
		// 콘서트 좌석 및 티켓 상태 업데이트
		info.getConcertSeat().reserve();
		info.getTicketReservation().approve();

		// 상태 변경 및 DTO 반환
		concertSeatRepository.save(info.getConcertSeat());
		TicketReservation savedReservation = ticketReservationRepository.save(info.getTicketReservation());
		return TicketReservationToDto(savedReservation);
	}

	// 예약 취소 처리
	private TicketReservationInfoResponseDto processReservationCancellation(TicketReservationServiceFindInfo info) {
		// 좌석 및 티켓 예약 취소
		info.getConcertSeat().cancel();
		info.getTicketReservation().cancel();
		// 콘서트 예약가능좌석 수 변동
		info.getConcert().cancelSeat();

		// 저장 및 DTO 반환
		concertSeatRepository.save(info.getConcertSeat());
		concertRepository.save(info.getConcert());
		TicketReservation savedReservation = ticketReservationRepository.save(info.getTicketReservation());
		return TicketReservationToDto(savedReservation);
	}

	// 좌석 예약 처리
	private TicketReservationInfoResponseDto processSeatReservation(TicketReservationServiceFindInfo info) {
		info.getConcertSeat().pending();
		info.getConcert().reserveSeat();

		TicketReservation reservation = TicketReservation.builder()
			.member(info.getMember())
			.seat(info.getConcertSeat())
			.status(TicketReservationStatus.PENDING)
			.build();

		TicketReservation savedReservation = ticketReservationRepository.save(reservation);
		concertSeatRepository.save(info.getConcertSeat());
		concertRepository.save(info.getConcert());
		return TicketReservationToDto(savedReservation);
	}

	// 웨이팅 등록 처리
	public TicketWaitingResponseDto registerWaiting(TicketReservationServiceFindInfo info) {
		Integer maxWaitingNumber = ticketWaitingRepository.findMaxWaitingNumberByConcertId(info.getConcert().getId())
			.orElse(0);
		Integer waitingNumber = maxWaitingNumber + 1;

		TicketWaiting waiting = TicketWaiting.builder()
			.concert(info.getConcert())
			.member(info.getMember())
			.waitingNumber(waitingNumber)
			.status(TicketWaitingStatus.WAITING)
			.build();

		TicketWaiting savedWaiting = ticketWaitingRepository.save(waiting);
		return TicketWaitingResponseToDto(savedWaiting, waitingNumber);
	}


}

