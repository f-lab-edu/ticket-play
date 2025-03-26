package com.flab.tiple.concert.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class ConcertNotStartDateException extends BusinessException {
	public ConcertNotStartDateException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}