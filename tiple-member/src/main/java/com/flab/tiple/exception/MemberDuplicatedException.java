package com.flab.tiple.exception;


public class MemberDuplicatedException extends BusinessException {
    public MemberDuplicatedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
