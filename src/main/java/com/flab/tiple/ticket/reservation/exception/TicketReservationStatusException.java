package com.flab.tiple.ticket.reservation.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class TicketReservationStatusException  extends BusinessException {
	public TicketReservationStatusException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
