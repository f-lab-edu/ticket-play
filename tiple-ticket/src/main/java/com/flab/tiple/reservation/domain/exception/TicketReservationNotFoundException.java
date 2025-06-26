package com.flab.tiple.reservation.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class TicketReservationNotFoundException extends BusinessException {
	public TicketReservationNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
