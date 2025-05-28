package com.flab.tiple.auth.application.service;

import com.flab.tiple.auth.application.dto.request.LoginRequestDto;
import com.flab.tiple.auth.application.dto.response.LoginResponseDto;

public interface AuthService {
	LoginResponseDto login(LoginRequestDto loginRequestDto);
}
