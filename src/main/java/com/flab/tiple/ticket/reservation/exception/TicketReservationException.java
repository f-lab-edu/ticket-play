package com.flab.tiple.ticket.reservation.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class TicketReservationException extends BusinessException {
	public TicketReservationException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
