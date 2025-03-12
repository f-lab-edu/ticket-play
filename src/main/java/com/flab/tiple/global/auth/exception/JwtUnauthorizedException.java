package com.flab.tiple.global.auth.exception;

import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.exception.TokenException;

public class JwtUnauthorizedException extends TokenException {
	public JwtUnauthorizedException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
