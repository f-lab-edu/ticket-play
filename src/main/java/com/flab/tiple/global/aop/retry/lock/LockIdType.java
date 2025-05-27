package com.flab.tiple.global.aop.retry.lock;


/**
 * PARAMETER: 메서드의 특정 파라미터 값을 락 ID로 사용
 * METHOD_NAME: 메서드 이름을 락 ID로 사용 (파라미터 기반 락이 불필요한 경우)
 */
public enum LockIdType {
	PARAMETER,    // 파라미터에서 ID 추출
	METHOD_NAME,  // 메서드 이름 사용
}
