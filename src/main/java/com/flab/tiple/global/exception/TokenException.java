package com.flab.tiple.global.exception;

import lombok.Getter;

@Getter
public class TokenException extends RuntimeException {

	private final ErrorCode errorCode;

	public TokenException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
