package com.flab.tiple.ticket.waiting.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class TicketWaitingRegisterException extends BusinessException {
	public TicketWaitingRegisterException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
