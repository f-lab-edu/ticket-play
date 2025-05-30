package com.flab.tiple.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class ConcertClosedException extends BusinessException {
	public ConcertClosedException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}

