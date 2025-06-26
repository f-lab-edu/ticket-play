package com.flab.tiple.waiting.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class TicketWaitingNotFoundException  extends BusinessException {
	public TicketWaitingNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
