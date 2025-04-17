package com.flab.tiple.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedissonLockAspect {

	private final RedissonClient redissonClient;

	@Around("@annotation(com.flab.tiple.global.util.RedissonLock)")
	public Object redissonLock(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		RedissonLock annotation = method.getAnnotation(RedissonLock.class);

		// 락 키 생성
		String lockKey = method.getName() + ":" + CustomSpringELParser.getDynamicValue(
			signature.getParameterNames(),
			joinPoint.getArgs(),
			annotation.value()
		);

		log.info("Trying to acquire lock: {}", lockKey);
		RLock lock = redissonClient.getLock(lockKey);
		boolean isLocked = false;

		try {
			// 락 획득 시도 (밀리초 단위)
			isLocked = lock.tryLock(annotation.waitTime(), annotation.leaseTime(), TimeUnit.MILLISECONDS);

			if (!isLocked) {
				log.info("Failed to acquire lock: {}", lockKey);
				throw new IllegalStateException("락 획득 실패, 잠시 후 다시 시도해주세요.");
			}

			log.info("Lock acquired, executing method: {}", method.getName());
			return joinPoint.proceed(); // 메서드 실행 결과 반환
		} catch (InterruptedException e) {
			log.error("Lock interrupted: {}", lockKey, e);
			Thread.currentThread().interrupt();
			throw e;
		} finally {
			if (isLocked && lock.isHeldByCurrentThread()) {
				lock.unlock();
				log.info("Lock released: {}", lockKey);
			}
		}
	}
}