package com.flab.tiple.ticket.waiting.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class TicketWaitingException extends BusinessException {
	public TicketWaitingException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
