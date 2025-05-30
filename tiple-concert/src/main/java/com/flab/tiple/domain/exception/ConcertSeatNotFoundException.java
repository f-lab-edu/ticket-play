package com.flab.tiple.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class ConcertSeatNotFoundException extends BusinessException {
	public ConcertSeatNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
