package com.flab.tiple.ticket.waiting.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class TicketWaitingNotCancelableException extends BusinessException {
	public TicketWaitingNotCancelableException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}