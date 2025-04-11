package com.flab.tiple.global.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * MySQL의 Named Lock을 사용하여 분산 락을 제공하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MySQLLockService {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * Named Lock 획득
	 *
	 * @param lockName 락 이름
	 * @param timeoutSeconds 타임아웃 (초)
	 * @return 락 획득 성공 여부
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean acquireLock(String lockName, int timeoutSeconds) {
		try {
			//MySQL의 GET_LOCK 함수로 락을 시도합니다.
			Integer result = jdbcTemplate.queryForObject(
				"SELECT GET_LOCK(?, ?)",
				Integer.class,
				lockName,
				timeoutSeconds
			);
			//1 반환 시 락 획득 성공, 0은 실패, NULL은 오류입니다.
			boolean acquired = result != null && result == 1;
			if (acquired) {
				log.info("Lock acquired: {}", lockName);
			} else {
				log.warn("Failed to acquire lock: {}", lockName);
			}
			return acquired;
		} catch (Exception e) {
			log.error("Error acquiring lock: {}", lockName, e);
			return false;
		}
	}

	/**
	 * Named Lock 해제
	 *
	 * @param lockName 락 이름
	 * @return 락 해제 성공 여부
	 */
	//이 메서드는 항상 새로운 트랜잭션으로 실행됩니다. 독립적인 트랜잭션으로 락을 얻습니다.
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean releaseLock(String lockName) {
		try {
			//MySQL의 RELEASE_LOCK 함수를 사용해서 락 해제 시도합니다.
			Integer result = jdbcTemplate.queryForObject(
				"SELECT RELEASE_LOCK(?)",
				Integer.class,
				lockName
			);
			//성공적으로 해제되었는지 확인.
			boolean released = result != null && result == 1;

			//락이 성공적으로 획득되었는지 boolean으로 판별합니다.
			if (released) {
				log.info("Lock released: {}", lockName);
			} else {
				log.warn("Failed to release lock: {}", lockName);
			}
			return released;
		} catch (Exception e) {
			log.error("Error releasing lock: {}", lockName, e);
			return false;
		}
	}

	/**
	 * 락을 획득하고 작업을 실행한 후 락을 해제
	 *
	 * @param lockName 락 이름
	 * @param timeoutSeconds 타임아웃 (초)
	 * @param supplier 락 획득 후 실행할 작업
	 * @param <T> 반환 타입
	 * @return 작업 실행 결과
	 * @throws RuntimeException 락 획득 실패 또는 작업 실행 중 예외 발생
	 */
	// Supplier<T>: 실행할 작업을 함수형 인터페이스로 전달받습니다.
	public <T> T executeWithLock(String lockName, int timeoutSeconds, Supplier<T> supplier) {
		boolean lockAcquired = false;
		try {
			// 먼저 락 획득 시도. 성공 여부를 lockAcquired에 저장.
			lockAcquired = acquireLock(lockName, timeoutSeconds);
			if (!lockAcquired) {
				throw new LockAcquisitionException("Failed to acquire lock: " + lockName);
			}
			// 락 실패 시 예외 발생.
			return supplier.get();
		} finally {
			// 락을 성공적으로 얻은 경우, 실제 작업 실행.
			// 예외 발생 여부와 무관하게, 락이 있었다면 해제합니다.
			if (lockAcquired) {
				releaseLock(lockName);
			}
		}
	}

	/**
	 * 락 획득 실패 시 발생하는 예외
	 */
	public static class LockAcquisitionException extends RuntimeException {
		public LockAcquisitionException(String message) {
			super(message);
		}

		public LockAcquisitionException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}