package com.flab.tiple.reservation.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class TicketReservationException extends BusinessException {
	public TicketReservationException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
