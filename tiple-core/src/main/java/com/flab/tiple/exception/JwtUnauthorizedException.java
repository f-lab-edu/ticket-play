package com.flab.tiple.exception;

public class JwtUnauthorizedException extends TokenException {
	public JwtUnauthorizedException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
