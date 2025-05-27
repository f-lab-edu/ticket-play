package com.flab.tiple.application.service;

import com.flab.tiple.application.dto.request.LoginRequestDto;
import com.flab.tiple.application.dto.response.LoginResponseDto;

public interface AuthService {
	LoginResponseDto login(LoginRequestDto loginRequestDto);
}
