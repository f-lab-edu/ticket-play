package com.flab.tiple.global.util;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributeLockExecutorWithRedisson {

	private final RedissonClient redissonClient;

	/**
	 * Redisson을 사용하여 분산 락을 처리하는 컴포넌트 클래스
	 *
	 * @param lockName            락의 이름
	 * @param waitMilliSecond     락을 기다리는 최대 시간 (밀리초)
	 * @param leaseMilliSecond    락의 보유 시간 (밀리초)
	 * @param runnable            락 획득 후 실행할 작업
	 * @throws InterruptedException 락을 기다리는 동안 인터럽트가 발생한 경우
	 * @throws IllegalStateException 락 획득에 실패한 경우
	 */
	public void execute(String lockName, long waitMilliSecond, long leaseMilliSecond, Runnable runnable) {
		RLock lock = redissonClient.getLock(lockName);
		boolean locked = false;

		try {
			// 락을 기다리는 최대 시간(waitMilliSecond)동안 락 획득 시도
			locked = lock.tryLock(waitMilliSecond, leaseMilliSecond, TimeUnit.MILLISECONDS);

			if (!locked) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Redisson 락 획득 실패 [ " + lockName + " ]");
			}

			// 락 획득 후 실행할 작업 실행
			runnable.run();

		} catch (InterruptedException e) {
			log.error("락 획득 중 인터럽트 발생: " + e.getMessage(), e);
			Thread.currentThread().interrupt();
			throw new IllegalStateException("락 획득 중 인터럽트 발생 [ " + lockName + " ]", e);
		} finally {
			// 락 해제 - isLocked/isHeldByCurrentThread 호출 시에도 예외가 발생할 수 있으므로
			// locked 플래그를 통해 안전하게 처리
			if (locked) {
				try {
					lock.unlock();
				} catch (Exception e) {
					log.error("락 해제 중 예외 발생: " + e.getMessage(), e);
				}
			}
		}
	}
}