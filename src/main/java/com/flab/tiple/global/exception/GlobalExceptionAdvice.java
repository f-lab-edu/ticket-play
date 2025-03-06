package com.flab.tiple.global.exception;

import com.flab.tiple.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdvice {


    @ExceptionHandler(BusinessException.class)
    public ApiResponse<ErrorResponse> handleBusinessException(BusinessException e) {
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
