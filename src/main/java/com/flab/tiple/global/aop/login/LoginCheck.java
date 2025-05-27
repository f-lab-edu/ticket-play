package com.flab.tiple.global.aop.login;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Retention(RetentionPolicy.RUNTIME) : 어노테이션의 생명주기(유지 범위)를 결정.
 *  1.SOURCE: 소스 코드에만 존재, 컴파일 시 제거 (주석 수준)
 *  2.CLASS: 컴파일된 클래스 파일에 존재, but 런타임에는 접근 불가
 *  3.RUNTIME: 런타임 시점까지 유지, 리플렉션을 통해 접근 가능

 *   RUNTIME을 사용하는 이유:
 *   AOP에서 런타임에 어노테이션 정보를 읽어야 하기 때문 ->리플렉션을 통해 메서드의 어노테이션을 검사하고 특정 로직을 수행할 수 있음
 *
 *
 * @Target(ElementType.METHOD)
 *   어노테이션을 어디에 적용할 수 있는지 제한. -> ElementType.METHOD는 메서드에만 어노테이션을 사용할 수 있다는 의미
 *
 *    ElementType.TYPE: 클래스, 인터페이스에 적용
 *    ElementType.FIELD: 필드에 적용
 *    ElementType.PARAMETER: 메서드 파라미터에 적용
 *
 * 여기서 METHOD를 선택한 이유:
 * 로그인 체크를 메서드 단위로 제어하기 위함 -> 특정 메서드에만 로그인 필요 여부를 설정하고 싶을 때 사용
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LoginCheck {
	/**
	 * required 어노테이션 사용시 필수 여부 지정
	 *   @LoginCheck(required = false) // 로그인 선택적
	 *   @LoginCheck // required 기본값인 true 적용
	 */
	boolean required() default true;
}
