package com.flab.tiple.util;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Component
@RequiredArgsConstructor
@Slf4j
public class RedissionLockExecutor {

	private final RedissonClient redissonClient;

	/**
	 * 기존 Runnable 방식 (void 반환)
	 */
	public void execute(String lockName, long waitMilliSecond, long leaseMilliSecond, Runnable runnable) {
		executeWithLock(lockName, waitMilliSecond, leaseMilliSecond, () -> {
			runnable.run();
			return null;
		});
	}

	/**
	 * 제네릭 방식 (값 반환 가능)
	 */
	public <T> T executeWithLock(String lockName, long waitMilliSecond, long leaseMilliSecond, Supplier<T> supplier) {
		RLock lock = redissonClient.getLock(lockName);
		boolean locked = false;

		try {
			log.debug("락 획득 시도: {}", lockName);

			// 락을 기다리는 최대 시간(waitMilliSecond)동안 락 획득 시도
			locked = lock.tryLock(waitMilliSecond, leaseMilliSecond, TimeUnit.MILLISECONDS);

			if (!locked) {
				log.warn("락 획득 실패: {}", lockName);
				throw new IllegalStateException("Redisson 락 획득 실패 [ " + lockName + " ]");
			}

			log.debug("락 획득 성공: {}", lockName);

			// 락 획득 후 실행할 작업 실행
			return supplier.get();

		} catch (InterruptedException e) {
			log.error("락 획득 중 인터럽트 발생: {}, 에러: {}", lockName, e.getMessage());
			Thread.currentThread().interrupt();
			throw new IllegalStateException("락 획득 중 인터럽트 발생 [ " + lockName + " ]", e);
		} catch (Exception e) {
			log.error("락 실행 중 예외 발생: {}, 에러: {}", lockName, e.getMessage());
			throw e;
		} finally {
			// 락 해제 - 현재 스레드가 보유한 락인지 확인 후 해제
			if (locked && lock.isHeldByCurrentThread()) {
				try {
					lock.unlock();
					log.debug("락 해제 완료: {}", lockName);
				} catch (Exception e) {
					log.error("락 해제 중 예외 발생: {}, 에러: {}", lockName, e.getMessage());
				}
			}
		}
	}

	/**
	 * 기본값을 사용하는 편의 메서드
	 */
	public void execute(String lockName, Runnable runnable) {
		execute(lockName, 10000L, 30000L, runnable);
	}

	public <T> T executeWithLock(String lockName, Supplier<T> supplier) {
		return executeWithLock(lockName, 10000L, 30000L, supplier);
	}
}