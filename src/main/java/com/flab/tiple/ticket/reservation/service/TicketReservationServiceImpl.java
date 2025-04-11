package com.flab.tiple.ticket.reservation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.flab.tiple.concert.domain.Concert;
import com.flab.tiple.concert.domain.ConcertSeat;
import com.flab.tiple.concert.dto.response.ConcertSeatInfo;
import com.flab.tiple.concert.exception.ConcertNotFoundException;
import com.flab.tiple.concert.exception.ConcertSeatNotFoundException;
import com.flab.tiple.concert.exception.ConcertSeatReservationException;
import com.flab.tiple.concert.repository.concert.ConcertRepository;
import com.flab.tiple.concert.repository.concertSeat.ConcertSeatRepository;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.service.MySQLLockService;
import com.flab.tiple.global.service.MySQLLockService.LockAcquisitionException;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.dto.response.MemberInfoDto;
import com.flab.tiple.member.exception.MemberNotFoundException;
import com.flab.tiple.member.repository.MemberRepository;
import com.flab.tiple.ticket.reservation.domain.TicketReservation;
import com.flab.tiple.ticket.reservation.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationInfoResponseDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationResponseDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationServiceFindInfo;
import com.flab.tiple.ticket.reservation.enums.TicketProcessStatus;
import com.flab.tiple.ticket.reservation.enums.TicketReservationStatus;
import com.flab.tiple.ticket.reservation.exception.TicketReservationException;
import com.flab.tiple.ticket.reservation.exception.TicketReservationNotFoundException;
import com.flab.tiple.ticket.reservation.repository.TicketReservationRepository;
import com.flab.tiple.ticket.waiting.domain.TicketWaiting;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingResponseDto;
import com.flab.tiple.ticket.waiting.enums.TicketWaitingStatus;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketReservationServiceImpl implements TicketReservationService {
	private final TicketReservationRepository ticketReservationRepository;
	private final ConcertSeatRepository concertSeatRepository;
	private final MemberRepository memberRepository;
	private final ConcertRepository concertRepository;
	private final TicketWaitingRepository ticketWaitingRepository;
	private final MySQLLockService mySQLLockService;

	private static final int LOCK_TIMEOUT_SECONDS = 10;
	private static final String SEAT_LOCK_PREFIX = "seat_lock_";
	private static final String RESERVATION_LOCK_PREFIX = "reservation_lock_";
	private static final String CONCERT_LOCK_PREFIX = "concert_lock_";
	private static final String WAITING_LOCK_PREFIX = "waiting_lock_";

	/**
	 * 티켓 예약 요청 처리
	 */
	@Transactional(isolation = Isolation.SERIALIZABLE)
	@Override
	public TicketReservationResponseDto<?> requestReservation(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		//이 좌석 ID를 기반으로 락 이름을 생성.
		String seatLockName = SEAT_LOCK_PREFIX + requestDto.getSeatId();

		try {
			// MySQL Named Lock을 사용하여 좌석에 대한 배타적 접근 보장
			//	 executeWithLock() 내부에서는 다음과 같이 동작:
			//   SELECT GET_LOCK('seat_lock_123', 10); -- 최대 10초 대기
			//   락을 잡은 스레드만 아래 로직을 실행할 수 있음.

			return mySQLLockService.executeWithLock(seatLockName, LOCK_TIMEOUT_SECONDS, () -> {
				try {
					// 락 획득 성공 → 좌석 예약 시도
					TicketReservationInfoResponseDto reservation = tryReserveSeat(requestDto, memberId);

					return TicketReservationResponseDto.builder()
						.data(reservation)
						.status(TicketProcessStatus.SUCCESS)
						.build();

				} catch (ConcertSeatReservationException e) {
					// 좌석 예약 실패 시 대기 처리
					TicketWaitingResponseDto waitingReservation = processWaitingRegistration(requestDto, memberId);
					return TicketReservationResponseDto.builder()
						.data(waitingReservation)
						.status(TicketProcessStatus.WAITING)
						.build();
				}
			});
		} catch (Exception e) {
			throw new TicketReservationException(ErrorCode.TICKET_RESERVATION_ERROR, ErrorCode.TICKET_RESERVATION_ERROR.getDescription());
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
		// 3. 정보 수정
		return processSeatReservation(info);
	}

	/**
	 * 티켓 예약 승인
	 */
	@Transactional
	@Override
	public TicketReservationInfoResponseDto approveReservation(Long reservationId, Long memberId) {
		String lockName = RESERVATION_LOCK_PREFIX + reservationId;

		return mySQLLockService.executeWithLock(lockName, LOCK_TIMEOUT_SECONDS, () -> {
			// 1. 정보 찾기
			TicketReservationServiceFindInfo info = findReservationInfo(reservationId, memberId);
			// 2. 정보 검증
			validateReservationApproval(info);
			// 3. 정보 수정
			TicketReservationInfoResponseDto result = processReservationApproval(info);
			return result;
		});
	}

	/**
	 * 티켓 예약 취소
	 */
	@Transactional
	@Override
	public TicketReservationInfoResponseDto cancelReservation(
		Long reservationId,
		Long memberId
	) {
		String lockName = RESERVATION_LOCK_PREFIX + reservationId;

		return mySQLLockService.executeWithLock(lockName, LOCK_TIMEOUT_SECONDS, () -> {
			// 1. 정보 찾기
			TicketReservationServiceFindInfo info = findReservationInfo(reservationId, memberId);
			// 2. 정보 검증
			validateReservationCancellation(info);
			// 3. 정보 수정
			TicketReservationInfoResponseDto result = processReservationCancellation(info);
			log.info("Successfully cancelled reservation with ID: {}", reservationId);
			return result;
		});
	}

	/**
	 * 사용자의 모든 예약 조회
	 */
	@Override
	public List<TicketReservationInfoResponseDto> getMemberReservations(Long memberId) {
		List<TicketReservation> ticketReservations = ticketReservationRepository.findByMemberId(memberId);
		return ticketReservations.stream().map(this::TicketReservationToDto).toList();
	}

	/**
	 * 대기 목록 등록 처리
	 */
	@Transactional(propagation = Propagation.SUPPORTS)
	public TicketWaitingResponseDto processWaitingRegistration(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		// 대기 목록 등록 시 콘서트 ID 락 사용
		ConcertSeat seat = concertSeatRepository.findById(requestDto.getSeatId())
			.orElseThrow(() -> new ConcertSeatNotFoundException(ErrorCode.CONCERT_SEAT_NOT_FOUND,
				ErrorCode.CONCERT_SEAT_NOT_FOUND.getDescription()));
		Long concertId = seat.getConcert().getId();

		String lockName = WAITING_LOCK_PREFIX + concertId;

		return mySQLLockService.executeWithLock(lockName, LOCK_TIMEOUT_SECONDS, () -> {
			// 1. 정보 찾기
			TicketReservationServiceFindInfo info = findSeatInfo(requestDto.getSeatId(), memberId);
			// 2. 정보 검증
			validateWaitingRegistration(info);
			// 3. 정보 수정
			TicketWaitingResponseDto result = registerWaiting(info);
			return result;
		});
	}

	// 대기 목록 등록
	@Transactional
	public TicketWaitingResponseDto registerWaiting(TicketReservationServiceFindInfo info) {
		// 대기 번호 조회 및 할당
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

	// 좌석 ID로 정보 조회 - FOR UPDATE 쿼리 사용하여 비관적 락 적용
	private TicketReservationServiceFindInfo findSeatInfo(Long seatId, Long memberId) {
		// 최신 상태의 좌석 정보 조회
		ConcertSeat seat = concertSeatRepository.findByIdForUpdate(seatId)
			.orElseThrow(() -> new ConcertSeatNotFoundException(ErrorCode.CONCERT_SEAT_NOT_FOUND,
				ErrorCode.CONCERT_SEAT_NOT_FOUND.getDescription()));

		// 최신 상태의 콘서트 정보 조회
		Concert concert = concertRepository.findByIdForUpdate(seat.getConcert().getId())
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
}