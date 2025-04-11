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
import com.flab.tiple.concert.exception.ConcertRemainSeatExistException;
import com.flab.tiple.concert.exception.ConcertSeatNotFoundException;
import com.flab.tiple.concert.exception.ConcertSeatReservationException;
import com.flab.tiple.concert.repository.concert.ConcertRepository;
import com.flab.tiple.concert.repository.concertSeat.ConcertSeatRepository;
import com.flab.tiple.global.exception.ErrorCode;
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
import com.flab.tiple.ticket.reservation.exception.TicketReservationNotFoundException;
import com.flab.tiple.ticket.reservation.repository.TicketReservationRepository;
import com.flab.tiple.ticket.waiting.domain.TicketWaiting;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingResponseDto;
import com.flab.tiple.ticket.waiting.enums.TicketWaitingStatus;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingRegisterException;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketReservationServiceImpl implements TicketReservationService {
	private final TicketReservationRepository ticketReservationRepository;
	private final ConcertSeatRepository concertSeatRepository;
	private final MemberRepository memberRepository;
	private final ConcertRepository concertRepository;
	private final TicketWaitingRepository ticketWaitingRepository;

	// 티켓 예약 요청
	@Transactional(isolation = Isolation.SERIALIZABLE)
	@Override
	public TicketReservationResponseDto<?> requestReservation(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		try {
			// 일반 예약 시도
			TicketReservationInfoResponseDto reservation = tryReserveSeat(requestDto, memberId);
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
		// 1. 정보 찾기 - Pessimistic Lock 적용
		TicketReservationServiceFindInfo info = findSeatInfoWithLock(requestDto.getSeatId(), memberId);
		// 2. 정보 검증
		validateSeatReservation(info);
		// 3. 정보 수정
		return processSeatReservation(info);
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public TicketWaitingResponseDto processWaitingRegistration(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		try {
			TicketReservationServiceFindInfo info = findReservationInfoWithLock(requestDto.getSeatId(), memberId);
			validateWaitingRegistration(info);
			return registerWaiting(info);
		} catch (ConcertRemainSeatExistException e) {
			throw new ConcertRemainSeatExistException(ErrorCode.CONCERT_REMAINING_SEAT_EXIST,
				ErrorCode.CONCERT_REMAINING_SEAT_EXIST.getDescription());
		} catch (Exception ex) {
			System.out.println("error:" + ex.getMessage());
			throw new TicketWaitingRegisterException(ErrorCode.TICKET_WAITING_ERROR,
				ErrorCode.TICKET_WAITING_ERROR.getDescription());
		}
	}

	// 예약 승인
	@Transactional
	@Override
	public TicketReservationInfoResponseDto approveReservation(Long reservationId, Long memberId) {
		// 1. 정보 찾기 - Pessimistic Lock 적용
		TicketReservationServiceFindInfo info = findReservationInfoWithLock(reservationId, memberId);
		// 2. 정보 검증
		validateReservationApproval(info);
		// 3. 정보 수정
		return processReservationApproval(info);
	}

	@Transactional
	@Override
	public TicketReservationInfoResponseDto cancelReservation(
		Long reservationId,
		Long memberId
	) {
		// 1. 정보 찾기 - Pessimistic Lock 적용
		TicketReservationServiceFindInfo info = findReservationInfoWithLock(reservationId, memberId);
		// 2. 정보 검증
		validateReservationCancellation(info);
		// 3. 정보 수정
		return processReservationCancellation(info);
	}

	@Override
	public List<TicketReservationInfoResponseDto> getMemberReservations(Long memberId) {
		List<TicketReservation> ticketReservations = ticketReservationRepository.findByMemberId(memberId);
		return ticketReservations.stream().map(this::TicketReservationToDto).toList();
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

	// 예약 ID로 정보 조회 (Pessimistic Lock 적용)
	private TicketReservationServiceFindInfo findReservationInfoWithLock(Long reservationId, Long memberId) {
		// 수정: findById -> findByIdWithPessimisticLock
		TicketReservation reservation = ticketReservationRepository.findByIdWithPessimisticLock(reservationId)
			.orElseThrow(() -> new TicketReservationNotFoundException(ErrorCode.TICKET_RESERVATION_NOT_FOUND,
				ErrorCode.TICKET_RESERVATION_NOT_FOUND.getDescription()));

		ConcertSeat concertSeat = concertSeatRepository.findByIdWithPessimisticLock(reservation.getSeat().getId())
			.orElseThrow(() -> new ConcertSeatNotFoundException(ErrorCode.CONCERT_SEAT_NOT_FOUND,
				ErrorCode.CONCERT_SEAT_NOT_FOUND.getDescription()));

		Concert concert = concertRepository.findByIdWithPessimisticLock(concertSeat.getConcert().getId())
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

	// 예약 ID로 정보 조회 (기존 메서드 - 락 없이)
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

	// 좌석 ID로 정보 조회 (Pessimistic Lock 적용)
	private TicketReservationServiceFindInfo findSeatInfoWithLock(Long seatId, Long memberId) {
		ConcertSeat seat = concertSeatRepository.findByIdWithPessimisticLock(seatId)
			.orElseThrow(() -> new ConcertSeatNotFoundException(ErrorCode.CONCERT_SEAT_NOT_FOUND,
				ErrorCode.CONCERT_SEAT_NOT_FOUND.getDescription()));

		Concert concert = concertRepository.findByIdWithPessimisticLock(seat.getConcert().getId())
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