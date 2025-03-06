package com.flab.tiple.global.exception;


import lombok.Getter;

@Getter
public enum ErrorCode {
    MEMBER_DUPLICATED("이 유저는 이미 가입된 멤버입니다", 400),
    MEMBER_NOT_FOUND("유저를 찾을 수 없습니다", 400);

    private final String description;
    private final int status;

    ErrorCode(String description, int status) {
        this.description = description;
        this.status = status;
    }
}
