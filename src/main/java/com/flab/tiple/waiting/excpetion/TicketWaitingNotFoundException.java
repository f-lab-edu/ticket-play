package com.flab.tiple.waiting.excpetion;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class TicketWaitingNotFoundException  extends BusinessException {
	public TicketWaitingNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
