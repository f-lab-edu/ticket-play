package com.flab.tiple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBoot를 시작하는 대표 어노테이션
 * @SpringBootConfiguration: @Configuration을 포함하는 Spring Boot 설정 어노테이션
 * @EnableAutoConfiguration: spring-boot-autoconfigure 패키지를 기반으로 자동 설정 수행
 * @ComponentScan: 현재 클래스가 위치한 패키지를 기준으로 하위 패키지를 모두 스캔하여 @Component, @Service, @Repository, @Controller 등의 Spring Bean을 등록.
 *
 * 추가 어노테이션
 * @Documented : 해당 어노테이션이 JavaDoc에 포함되도록 지정하는 메타 어노테이션 API 문서에 포함하여 문서화할 때 사용
 * @Inherited : 어노테이션이 상속될 수 있도록 지정하는 메타 어노테이션
 * @Retention(RetentionPolicy.RUNTIME): 어노테이션의 유효 범위(라이프사이클) 를 설정하는 메타 어노테이션
 * RetentionPolicy.SOURCE → 컴파일 시 제거 (@Override, @SuppressWarnings 등)
 * RetentionPolicy.CLASS → 클래스 파일에는 남아있지만 런타임 시에는 사용 불가능
 * RetentionPolicy.RUNTIME → 런타임까지 유지되며 리플렉션(Reflection)으로 접근 가능 (Spring, JPA 등에서 주로 사용)
 *
 * @Target(ElementType.TYPE)
 * 설명: 어노테이션이 적용될 대상(ElementType) 을 지정하는 메타 어노테이션
 * 자주 사용하는 값:
 * ElementType.TYPE → 클래스, 인터페이스, 열거형(enum)에 적용 가능
 * ElementType.METHOD → 메서드에 적용 가능
 * ElementType.FIELD → 필드(멤버 변수)에 적용 가능
 * ElementType.PARAMETER → 메서드의 매개변수에 적용 가능
 * ElementType.ANNOTATION_TYPE → 어노테이션에 적용 가능 (메타 어노테이션)
 */
@SpringBootApplication
public class TipleApplication {
	public static void main(String[] args) {
		SpringApplication.run(TipleApplication.class, args);
	}
}
