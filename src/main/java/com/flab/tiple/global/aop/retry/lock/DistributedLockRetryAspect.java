package com.flab.tiple.global.aop.retry.lock;

import java.util.concurrent.atomic.AtomicReference;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.util.RedissionLockExecutor;
import com.flab.tiple.ticket.reservation.application.dto.request.TicketReservationRequestDto;
import com.flab.tiple.ticket.reservation.domain.exception.TicketReservationException;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingRegisterException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockRetryAspect {

	private final RetryTemplate reservationRetryTemplate;
	private final RedissionLockExecutor distributeLockExecutor;

	@Around("@annotation(distributedLockRetry)")
	public Object executeWithRetry(ProceedingJoinPoint joinPoint,
		DistributedLockRetry distributedLockRetry) throws Throwable {

		String primaryLockName = generateLockName(joinPoint, distributedLockRetry);

		return reservationRetryTemplate.execute(retryContext -> {
			log.info("분산 락 획득 시도 [{}] - 재시도 횟수: {}", primaryLockName, retryContext.getRetryCount());
			AtomicReference<Object> result = new AtomicReference<>();

			try {
				// 기본 락 획득
				distributeLockExecutor.execute(
					primaryLockName,
					distributedLockRetry.waitTimeMs(),
					distributedLockRetry.leaseTimeMs(),
					() -> {
						// 복합 락이 필요한 경우
						if (distributedLockRetry.compoundLock()) {
							executeWithSecondaryLock(joinPoint, distributedLockRetry, result);
						} else {
							// 단일 락만 필요한 경우
							executeBusinessLogic(joinPoint, result);
						}
					}
				);
				return result.get();
			} catch (Exception e) {
				log.warn("분산 락 획득 실패 [{}] - 재시도 예정", primaryLockName);
				throw e;
			}
		}, recoveryContext -> {
			log.error("최대 재시도 횟수 초과 [{}]", primaryLockName);
			throw new TicketReservationException(
				ErrorCode.TICKET_RESERVATION_ERROR,
				"요청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
			);
		});
	}

	/**
	 * 락 이름 생성 로직
	 */
	private String generateLockName(ProceedingJoinPoint joinPoint, DistributedLockRetry annotation) {
		Object[] args = joinPoint.getArgs();

		switch (annotation.lockIdType()) {
			case PARAMETER:
				return generateParameterBasedLockName(args, annotation);
			case METHOD_NAME:
				return generateMethodBasedLockName(joinPoint, annotation);
			default:
				throw new IllegalArgumentException("지원하지 않는 LockIdType: " + annotation.lockIdType());
		}
	}

	/**
	 * PARAMETER 타입: 특정 파라미터 값으로 락 이름 생성
	 */
	private String generateParameterBasedLockName(Object[] args, DistributedLockRetry annotation) {
		int idx = annotation.idParameterIndex();
		if (idx >= args.length) {
			throw new IllegalArgumentException("파라미터 인덱스가 범위를 벗어났습니다: " + idx);
		}

		Object paramValue = args[idx];
		if (paramValue instanceof Long) {
			return annotation.lockPrefix() + paramValue;
		} else if (paramValue instanceof TicketReservationRequestDto) {
			// DTO의 경우 특정 필드 값 사용
			TicketReservationRequestDto dto = (TicketReservationRequestDto) paramValue;
			return annotation.lockPrefix() + dto.getSeatId();
		} else {
			throw new IllegalArgumentException("지원하지 않는 파라미터 타입: " + paramValue.getClass());
		}
	}

	/**
	 * METHOD_NAME 타입: 메서드 이름으로 락 이름 생성
	 */
	private String generateMethodBasedLockName(ProceedingJoinPoint joinPoint, DistributedLockRetry annotation) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		return annotation.lockPrefix() + signature.getName();
	}


	/**
	 * 복합 락 처리: 두 번째 락까지 획득
	 */
	private void executeWithSecondaryLock(ProceedingJoinPoint joinPoint,
		DistributedLockRetry annotation,
		AtomicReference<Object> result) {
		String secondaryLockName = generateSecondaryLockName(joinPoint, annotation);

		try {
			distributeLockExecutor.execute(
				secondaryLockName,
				annotation.waitTimeMs(),
				annotation.leaseTimeMs(),
				() -> executeBusinessLogic(joinPoint, result)
			);
		} catch (Exception e) {
			log.error("보조 락 획득 실패: {}", secondaryLockName);
			throw new TicketWaitingRegisterException(
				ErrorCode.TICKET_WAITING_ERROR,
				"대기열 번호 할당 중 오류가 발생했습니다. 다시 시도해주세요."
			);
		}
	}

	/**
	 * 두 번째 락 이름 생성
	 */
	private String generateSecondaryLockName(ProceedingJoinPoint joinPoint, DistributedLockRetry annotation) {
		Object[] args = joinPoint.getArgs();

		// DTO에서 콘서트 ID 추출하는 예시
		if (args.length > 0 && args[0] instanceof TicketReservationRequestDto) {
			TicketReservationRequestDto dto = (TicketReservationRequestDto) args[0];
			return annotation.secondaryLockPrefix() + dto.getConcertId();
		}

		// 기본적으로는 메서드명 기반
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		return annotation.secondaryLockPrefix() + signature.getName();
	}

	/**
	 * 실제 비즈니스 로직 실행
	 */
	private void executeBusinessLogic(ProceedingJoinPoint joinPoint, AtomicReference<Object> result) {
		try {
			result.set(joinPoint.proceed());
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}