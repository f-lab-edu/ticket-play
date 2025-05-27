package com.flab.tiple.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class AuthenticationFailedException  extends BusinessException {
	public AuthenticationFailedException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
