package com.flab.tiple.reservation.domain.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class TicketReservationStatusException  extends BusinessException {
	public TicketReservationStatusException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
