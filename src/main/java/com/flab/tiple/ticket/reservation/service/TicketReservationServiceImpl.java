package com.flab.tiple.ticket.reservation.service;

import java.util.List;

import org.springframework.stereotype.Service;
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
	@Transactional
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

	@Transactional
	public TicketReservationInfoResponseDto tryReserveSeat(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		// 좌석 및 멤버 정보 조회
		ConcertSeat seat = concertSeatRepository.findById(requestDto.getSeatId())
			.orElseThrow(() -> new ConcertSeatNotFoundException(ErrorCode.CONCERT_SEAT_NOT_FOUND,
				ErrorCode.CONCERT_SEAT_NOT_FOUND.getDescription()));

		Concert concert = concertRepository.findById(seat.getConcert().getId())
			.orElseThrow(()-> new ConcertNotFoundException(ErrorCode.CONCERT_NOT_FOUND,
				ErrorCode.CONCERT_NOT_FOUND.getDescription()));

		Member member = memberRepository.findById(memberId)
			.orElseThrow(()-> new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND,
				ErrorCode.MEMBER_NOT_FOUND.getDescription()));

		//콘서트 예매가능한 상황인지 체크
		concert.reservationStatusCheck();

		// 새로운 예약 생성 및 좌석 상태 변경
		TicketReservation reservation = TicketReservation.builder()
			.member(member)
			.seat(seat)
			.status(TicketReservationStatus.PENDING)
			.build();

		seat.pending();
		//콘서트 예약가능좌석 수 변동
		concert.reserveSeat();

		TicketReservation savedReservation = ticketReservationRepository.save(reservation);
		concertSeatRepository.save(seat);
		concertRepository.save(concert);
		return TicketReservationToDto(savedReservation);
	}

	@Transactional
	public TicketWaitingResponseDto processWaitingRegistration(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		try {
			// 좌석 정보 조회
			ConcertSeat seat = concertSeatRepository.findById(requestDto.getSeatId())
				.orElseThrow(() -> new ConcertSeatNotFoundException(ErrorCode.CONCERT_SEAT_NOT_FOUND,
					ErrorCode.CONCERT_SEAT_NOT_FOUND.getDescription()));

			Concert concert = concertRepository.findById(seat.getConcert().getId())
				.orElseThrow(() -> new ConcertNotFoundException(ErrorCode.CONCERT_NOT_FOUND,
					ErrorCode.CONCERT_NOT_FOUND.getDescription()));

			Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND,
					ErrorCode.MEMBER_NOT_FOUND.getDescription()));

			// 웨이팅 가능 조건 체크
			concert.reserveSeatCheck();
			// 웨이팅 등록
			TicketWaitingResponseDto responseDto = registerWaiting(concert, member);
			return responseDto;
		} catch (ConcertRemainSeatExistException e) {
			throw new ConcertRemainSeatExistException(ErrorCode.CONCERT_REMAINING_SEAT_EXIST, ErrorCode.CONCERT_REMAINING_SEAT_EXIST.getDescription());
		} catch (Exception ex) {
			System.out.println("error:"+ex.getMessage());
			throw new TicketWaitingRegisterException(ErrorCode.TICKET_WAITING_ERROR, ErrorCode.TICKET_WAITING_ERROR.getDescription());
		}
	}

	// 웨이팅 등록 로직
	@Transactional
	public TicketWaitingResponseDto registerWaiting(Concert concert, Member member) {
		// 웨이팅 번호 부여
		Integer maxWaitingNumber = ticketWaitingRepository.findMaxWaitingNumberByConcertId(concert.getId())
			.orElse(0);
		Integer waitingNumber = maxWaitingNumber + 1;
		TicketWaiting waiting = TicketWaiting.builder()
			.concert(concert)
			.member(member)
			.waitingNumber(waitingNumber)
			.status(TicketWaitingStatus.WAITING)
			.build();
		TicketWaiting savedWaiting = ticketWaitingRepository.save(waiting);

		return TicketWaitingResponseToDto(savedWaiting,waitingNumber);
	}

	// 예약 승인
	@Transactional
	@Override
	public TicketReservationInfoResponseDto approveReservation(Long reservationId, Long memberId) {
		// 티켓 정보 및 예약한 티켓 좌석 및 멤버 정보 조회
		TicketReservation reservation = ticketReservationRepository.findById(reservationId)
			.orElseThrow(() -> new TicketReservationNotFoundException(ErrorCode.TICKET_RESERVATION_NOT_FOUND, ErrorCode.TICKET_RESERVATION_NOT_FOUND.getDescription()));

		ConcertSeat concertSeat = reservation.getSeat();

		Concert concert = concertRepository.findById(concertSeat.getConcert().getId())
			.orElseThrow(()-> new ConcertNotFoundException(ErrorCode.CONCERT_NOT_FOUND,ErrorCode.CONCERT_NOT_FOUND.getDescription()));

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND, ErrorCode.MEMBER_NOT_FOUND.getDescription()));

		//콘서트 예매가능한 상황인지 체크
		concert.reservationStatusCheck();

		// 검증(1. 예약한 유저 본인이 맞는지, 2. 예약 가능한 상태인지)
		reservation.checkMatchMember(member);
		reservation.checkStatus();
		// 콘서트 좌석 및 티켓 상태 업데이트
		concertSeat.reserve();
		reservation.approve();

		// 상태 변경 및 DTO 반환
		concertSeatRepository.save(concertSeat);
		TicketReservation savedReservation = ticketReservationRepository.save(reservation);
		return TicketReservationToDto(savedReservation);
	}


	@Transactional
	@Override
	public TicketReservationInfoResponseDto cancelReservation(
		Long reservationId,
		Long memberId
	) {
		// 티켓 정보 및 예약한 티켓 좌석 및 멤버 정보 조회
		TicketReservation reservation = ticketReservationRepository.findById(reservationId)
			.orElseThrow(() -> new TicketReservationNotFoundException(ErrorCode.TICKET_RESERVATION_NOT_FOUND, ErrorCode.TICKET_RESERVATION_NOT_FOUND.getDescription()));

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(ErrorCode.MEMBER_NOT_FOUND, ErrorCode.MEMBER_NOT_FOUND.getDescription()));

		ConcertSeat concertSeat = reservation.getSeat();

		Concert concert = concertRepository.findById(concertSeat.getConcert().getId())
			.orElseThrow(()-> new ConcertNotFoundException(ErrorCode.CONCERT_NOT_FOUND,ErrorCode.CONCERT_NOT_FOUND.getDescription()));

		//콘서트 예매가능한 상황인지 체크
		concert.reservationStatusCheck();
		//예약 취소 불가능 테스트 (콘서트 시작 시간 하루전일때)
		concert.cancelTimeAvailableCheck();

		// 검증(1. 예약한 유저 본인이 맞는지, 2. 취소 가능한 상태인지)
		reservation.checkMatchMember(member);
		reservation.checkCancelPossibleStatus();

		// 좌석 및 티켓 예약 취소
		concertSeat.cancel();
		reservation.cancel();

		//콘서트 예약가능좌석 수 변동
		concert.cancelSeat();

		// 저장 및 DTO 반환
		concertSeatRepository.save(concertSeat);
		TicketReservation savedReservation = ticketReservationRepository.save(reservation);
		return TicketReservationToDto(savedReservation);
	}

	@Override
	public List<TicketReservationInfoResponseDto> getMemberReservations(Long memberId) {
		List<TicketReservation> ticketReservations =  ticketReservationRepository.findByMemberId(memberId);
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

}
