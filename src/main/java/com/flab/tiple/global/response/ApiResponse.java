package com.flab.tiple.global.response;

import com.flab.tiple.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode,String message, T data) {
        return new ApiResponse<>(errorCode.getStatus(), message, data);
    }
}
