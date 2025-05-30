package com.flab.tiple.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class ConcertRemainSeatExistException extends BusinessException {
	public ConcertRemainSeatExistException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}

