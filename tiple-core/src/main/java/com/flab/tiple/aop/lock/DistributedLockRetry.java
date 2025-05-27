package com.flab.tiple.aop.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLockRetry {
	long waitTimeMs() default 1000L;
	long leaseTimeMs() default 3000L;
	String lockPrefix() default "";

	/**
	 * 의미: 하나의 메서드에서 두 개 이상의 락을 순차적으로 획득해야 할 때 사용
	 * 예시: 티켓 예약 시 좌석 락과 대기열 락을 모두 획득해야 하는 경우
	 */
	boolean compoundLock() default false;

	/**
	 * 의미: compoundLock이 true일 때 사용할 두 번째 락의 접두사
	 * 예시: 첫 번째 락이 "seat:123", 두 번째 락이 "waiting:456"인 경우
	 */
	String secondaryLockPrefix() default "";

	// 락 ID를 추출할 파라미터 인덱스
	int idParameterIndex() default 0;

	// 락 ID를 생성하는 방식
	LockIdType lockIdType() default LockIdType.PARAMETER;

}