package com.flab.tiple.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class ConcertCancelTimeException extends BusinessException {
	public ConcertCancelTimeException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
