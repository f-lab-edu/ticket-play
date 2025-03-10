package com.flab.tiple.global.response;

import com.flab.tiple.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *@NoArgsConstructor: 매개변수가 없는 기본 생성자를 자동으로 생성
 *@AllArgsConstructor: 모든 필드를 초기화하는 생성자를 제공
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String message;

    //제네릭(Generic) 타입 T를 사용하는 필드.
    //T는 실제 데이터 타입을 나타내는 변수 역할을 하며, 객체가 생성될 때 타입이 결정
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode,String message, T data) {
        return new ApiResponse<>(errorCode.getStatus(), message, data);
    }
}
