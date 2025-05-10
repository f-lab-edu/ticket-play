package com.flab.tiple.global.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;


import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DistributedLockService {

	private final RedissonClient redissonClient;
	private static final long DEFAULT_WAIT_TIME = 5L;
	private static final long DEFAULT_LEASE_TIME = 10L;
	private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

	/**
	 * 분산 락을 획득하고 작업을 실행한 후 락을 해제합니다.
	 *
	 * @param lockName 락 이름
	 * @param supplier 실행할 작업
	 * @param <T> 반환 타입
	 * @return 실행 결과
	 */
	public <T> T executeWithLock(String lockName, Supplier<T> supplier) {
		return executeWithLock(lockName, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, supplier);
	}

	/**
	 * 분산 락을 획득하고 작업을 실행한 후 락을 해제합니다.
	 *
	 * @param lockName 락 이름
	 * @param waitTime 락 획득 대기 시간
	 * @param leaseTime 락 유지 시간
	 * @param supplier 실행할 작업
	 * @param <T> 반환 타입
	 * @return 실행 결과
	 */
	public <T> T executeWithLock(String lockName, long waitTime, long leaseTime, Supplier<T> supplier) {
		RLock lock = redissonClient.getLock(lockName);
		try {
			boolean isLocked = lock.tryLock(waitTime, leaseTime, TIME_UNIT);
			if (!isLocked) {
				throw new IllegalStateException("Failed to acquire lock: " + lockName);
			}
			return supplier.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Lock interrupted: " + lockName, e);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	/**
	 * 락을 획득하고 작업을 실행한 후 락을 해제합니다 (반환값 없음).
	 *
	 * @param lockName 락 이름
	 * @param runnable 실행할 작업
	 */
	public void executeWithLock(String lockName, Runnable runnable) {
		executeWithLock(lockName, () -> {
			runnable.run();
			return null;
		});
	}
}