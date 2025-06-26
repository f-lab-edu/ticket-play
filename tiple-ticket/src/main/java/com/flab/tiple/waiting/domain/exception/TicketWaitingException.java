package com.flab.tiple.waiting.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class TicketWaitingException extends BusinessException {
	public TicketWaitingException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}