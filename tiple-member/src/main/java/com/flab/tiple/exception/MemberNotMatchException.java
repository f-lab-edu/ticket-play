package com.flab.tiple.exception;


public class MemberNotMatchException extends BusinessException {
	public MemberNotMatchException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}

