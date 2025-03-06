package com.flab.tiple.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdvice {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return createErrorResponse(e, e.getErrorCode());
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(Exception e, ErrorCode errorCode) {
        log.warn(e.getClass().getName(), e);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(errorCode)
                .message(e.getMessage())
                .build();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(errorResponse);
    }
}
