package com.flab.tiple.exception;

import javax.security.sasl.AuthenticationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.flab.tiple.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @RestControllerAdvice: @ControllerAdvice와 @ResponseBody를 합친 기능을 수행
 * 모든 @RestController에서 발생하는 예외를 한 곳에서 처리할 수 있음.
 *
 * @Slf4j:Lombok에서 제공하는 로깅 어노테이션으로, log 객체를 자동으로 생성해줌
 * slf4j는 **로깅 수준(level)**을 설정하여 로그의 세부적인 제어가 가능
 *
 * 직접 콘솔로 찍는것과 Log객체를 사용하는 것의 차이
 * ***콘솔: 상대적으로 메모리 소모가 적음(출력만함)***
 * 성능: 따로 저장할 수 없고 대량 출력 시 성능 저하가 발생할 수 있습니다. 특히, 멀티스레드 환경에서는 성능 이슈가 발생할 수 있음.
 * Q. 콘솔이 대량 출력 시 성능 저하가 발생하고, 특히 멀티스레드 환경에서 성능 이슈를 일으킬 수 있는 이유
 * 1. 콘솔 I/O의 속도 제한 :  I/O 작업은 시간이 걸리고 비용이 발생, 대량의 데이터를 한 번에 출력하려고 할 때, 출력이 디스크에 기록되거나 콘솔 화면을 갱신하는 데 시간이 소요
 * 2. 동기화 문제 (멀티스레드 환경) : 내부적으로 System.out은 싱글톤 객체로, 여러 스레드에서 동시에 접근할 경우 동기화(synchronization) 문제가 발생할 수 있음
 * 3. 콘솔 출력의 버퍼링 문제 : 콘솔 출력은 기본적으로 버퍼링된 방식으로 처리되지만, System.out.println()이 호출될 때마다 버퍼를 비우는 작업이 발생할 수 있음
 *
 * *** @Slf4j / Logger : log객체를 생성해야 함으로 콘솔보다는 메모리 사용량이 높음 ***
 * SLF4J는 로깅 API로서 Logback 또는 다른 로깅 구현체가 실제 로그를 기록
 * @Slf4j를 사용하면 클래스마다 log라는 이름의 Logger 객체가 자동으로 생성, 설정한 클래스에 대해 하나의 인스턴스가 공유되어 사용
 * 성능: 로그 레벨 설정가능, 파일로 출력 가능, 비동기 출력 가능
 *
 * 비동기 로깅이 가능한 이유: 로그 메시지가 직접 콘솔이나 파일에 기록되는 것이 아니라 먼저 메모리 버퍼에 저장되었다가
 * 전용 스레드가 이 버퍼에서 로그 메시지를 비동기적으로 처리하여 실제 로그 파일에 기록
 *
 * @ExceptionHandler: 특정 예외를 처리하는 메서드에 붙이는 어노테이션.
 * @RestControllerAdvice 또는 @ControllerAdvice와 함께 사용하면 글로벌 예외 처리 기능을 제공
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdvice {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException ex) {
        // 여기에서 오류 메시지를 하드코딩하여 테스트
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse<>(401, "Authentication failed", null));
    }
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<ErrorResponse> handleBusinessException(BusinessException e) {
        return createErrorResponse(e, e.getErrorCode());
    }

    @ExceptionHandler(TokenException.class)
    public ApiResponse<ErrorResponse> handleTokenException(TokenException e) {
        return createErrorResponse(e, e.getErrorCode());
    }

    private ApiResponse<ErrorResponse> createErrorResponse(Exception e, ErrorCode errorCode) {
        log.warn(e.getClass().getName(), e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(errorCode)
                .message(errorCode.getDescription())
                .build();

        return ApiResponse.error(errorCode,e.getMessage(), errorResponse);
    }
}
