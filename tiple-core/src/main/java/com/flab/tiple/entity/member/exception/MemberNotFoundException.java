package com.flab.tiple.entity.member.exception;

import com.flab.tiple.exception.BusinessException;
import com.flab.tiple.exception.ErrorCode;

public class MemberNotFoundException  extends BusinessException {
	public MemberNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}

