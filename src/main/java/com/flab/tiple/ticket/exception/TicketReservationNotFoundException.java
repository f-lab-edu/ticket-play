package com.flab.tiple.ticket.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class TicketReservationNotFoundException extends BusinessException {
	public TicketReservationNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
