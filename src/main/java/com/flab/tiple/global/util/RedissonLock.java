package com.flab.tiple.global.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedissonLock {

	String value(); // Lock의 이름 (고유값)
	/**
	 * 락을 기다리는 시간 (밀리초 단위, 기본값: 5000ms)
	 */
	long waitTime() default 3000L;

	/**
	 * 락을 유지하는 시간 (밀리초 단위, 기본값: 10000ms)
	 */
	long leaseTime() default 3000L;

}
