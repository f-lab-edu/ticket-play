package com.flab.tiple.waiting.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class TicketWaitingStatusInvalidException  extends BusinessException {
	public TicketWaitingStatusInvalidException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}

