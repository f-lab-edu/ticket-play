package com.flab.tiple.global.exception;


import lombok.Getter;

@Getter
public enum ErrorCode {
    MEMBER_DUPLICATED("이 유저는 이미 가입된 멤버입니다", 400),
    MEMBER_NOT_FOUND("유저를 찾을 수 없습니다", 400),

    // 인증관련에러
    UNAUTHORIZED("인증실패", 401),
    TOKEN_EXPIRED("토큰이 만료되었습니다.",401),
    TOKEN_INVALID("유효하지 않은 토큰입니다.",401),
    TOKEN_MALFORMED("잘못된 형식의 토큰입니다.",401),
    TOKEN_ERROR("토큰 에러입니다.",401);

    private final String description;
    private final int status;

    ErrorCode(String description, int status) {
        this.description = description;
        this.status = status;
    }
}
