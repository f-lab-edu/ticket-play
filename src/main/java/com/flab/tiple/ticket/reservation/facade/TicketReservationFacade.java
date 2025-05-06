package com.flab.tiple.ticket.reservation.facade;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.flab.tiple.concert.exception.ConcertRemainSeatExistException;
import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.util.RedissionLockExecutor;
import com.flab.tiple.ticket.reservation.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.reservation.dto.response.TicketReservationResponseDto;
import com.flab.tiple.ticket.reservation.service.TicketReservationService;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingRegisterException;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketReservationFacade {
	private final TicketReservationService ticketReservationService;
	private final RedissionLockExecutor distributeLockExecutor;
	private final MeterRegistry meterRegistry;

	public TicketReservationResponseDto<?> requestReservationFacade(
		TicketReservationRequestDto requestDto,
		Long memberId
	) {
		// 좌석에 대한 락
		String seatLockName = "lockTicketReservation:" + requestDto.getSeatId();
		final long waitTime = 10000; // 10초
		final long leaseTime = 30000; // 30초

		// 결과를 저장할 변수
		final AtomicReference<TicketReservationResponseDto<?>> responseDto = new AtomicReference<>();

		long startTime = System.currentTimeMillis();
		AtomicLong firstLockAcquireTime = new AtomicLong();
		AtomicLong secondLockAcquireTime = new AtomicLong();
		long businessLogicTime = 0;

		try {
			// 첫 번째 락 획득 시작
			long firstLockStartTime = System.currentTimeMillis();
			distributeLockExecutor.execute(seatLockName, waitTime, leaseTime, () -> {
				// 첫 번째 락 획득 완료
				firstLockAcquireTime.set(System.currentTimeMillis() - firstLockStartTime);

				// 대기열 번호에 대한 락
				String concertWaitingLockKey = "lock:waiting:" + requestDto.getConcertId();

				try {
					long secondLockStartTime = System.currentTimeMillis();
					distributeLockExecutor.execute(concertWaitingLockKey, waitTime, leaseTime, () -> {
						// 비즈니스 로직 실행 시작
						secondLockAcquireTime.set(System.currentTimeMillis() - secondLockStartTime);

						// 실제 비즈니스 로직 실행
						responseDto.set(ticketReservationService.requestReservation(requestDto, memberId));

					});
				} catch (IllegalStateException e) {
					log.error("대기열 락 획득 실패: {}", concertWaitingLockKey);
					throw new TicketWaitingRegisterException(
						ErrorCode.TICKET_WAITING_ERROR,
						"대기열 번호 할당 중 오류가 발생했습니다. 다시 시도해주세요."
					);
				}
			});

			long totalTime = System.currentTimeMillis() - startTime;
			long totalLockTimeValue = firstLockAcquireTime.get() + secondLockAcquireTime.get();

			// CPU 사용량
			double processCpuUsage = meterRegistry.get("process.cpu.usage").gauge().value() * 100;
			double systemCpuUsage = meterRegistry.get("system.cpu.usage").gauge().value() * 100;

			MDC.put("totalTime", String.valueOf(totalTime));
			MDC.put("lockTime", String.valueOf(totalLockTimeValue));
			MDC.put("firstLockTime", String.valueOf(firstLockAcquireTime));
			MDC.put("secondLockTime", String.valueOf(secondLockAcquireTime));

			log.info("티켓 예약 요청 완료 시간: {}ms, 티켓 예약 요청 락타임:{},first lock: {}ms, second lock:{}ms  " +
					"Process CPU: {}%, System CPU: {}%",
				totalTime, totalLockTimeValue, firstLockAcquireTime, secondLockAcquireTime, processCpuUsage, systemCpuUsage);

			return responseDto.get();
		} catch (IllegalStateException e) {
			// 좌석 락 획득 실패 시
			log.error("좌석 락 획득 실패: {}", seatLockName);
			throw new ConcertRemainSeatExistException(
				ErrorCode.CONCERT_SEAT_RESERVATION_NOT_POSSIBLE,
				"현재 다른 사용자가 같은 좌석을 처리 중입니다. 잠시 후 다시 시도해주세요."
			);
		}
	}
}