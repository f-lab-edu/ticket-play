package com.flab.tiple.ticket.reservation.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class TicketReservationApprovalException extends BusinessException {
	public TicketReservationApprovalException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}

