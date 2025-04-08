package com.flab.tiple.concert.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class ConcertSeatReservationException extends BusinessException {
	public ConcertSeatReservationException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}

