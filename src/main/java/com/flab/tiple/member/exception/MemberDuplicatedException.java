package com.flab.tiple.member.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class MemberDuplicatedException extends BusinessException {
    public MemberDuplicatedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
