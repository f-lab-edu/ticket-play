package com.flab.tiple.member.exception;

import com.flab.tiple.global.exception.BusinessException;
import com.flab.tiple.global.exception.ErrorCode;

public class MemberNotFoundException  extends BusinessException {
	public MemberNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}

