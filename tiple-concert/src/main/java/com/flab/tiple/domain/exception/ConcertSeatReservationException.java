package com.flab.tiple.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class ConcertSeatReservationException extends BusinessException {
	public ConcertSeatReservationException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}

