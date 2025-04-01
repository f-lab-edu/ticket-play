package com.flab.tiple.member.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class MemberNotMatchException extends BusinessException {
	public MemberNotMatchException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}


