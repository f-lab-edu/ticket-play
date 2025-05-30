package com.flab.tiple.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class ConcertNotStartDateException extends BusinessException {
	public ConcertNotStartDateException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}