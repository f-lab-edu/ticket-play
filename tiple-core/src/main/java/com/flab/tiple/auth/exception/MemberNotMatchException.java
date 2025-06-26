package com.flab.tiple.auth.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class MemberNotMatchException extends BusinessException {
	public MemberNotMatchException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}

