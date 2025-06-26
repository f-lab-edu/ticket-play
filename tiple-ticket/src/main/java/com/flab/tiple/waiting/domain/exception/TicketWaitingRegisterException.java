package com.flab.tiple.waiting.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class TicketWaitingRegisterException extends BusinessException {
	public TicketWaitingRegisterException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
