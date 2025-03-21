package com.flab.tiple.concert.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class ConcertRemainSeatExistException extends BusinessException {
	public ConcertRemainSeatExistException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
