package com.flab.tiple.waiting.excpetion;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class TicketWaitingStatusInvalidException  extends BusinessException {
	public TicketWaitingStatusInvalidException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
