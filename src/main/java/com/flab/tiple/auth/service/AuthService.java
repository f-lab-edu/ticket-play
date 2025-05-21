package com.flab.tiple.auth.service;

import com.flab.tiple.auth.dto.request.LoginRequestDto;
import com.flab.tiple.auth.dto.response.LoginResponseDto;

public interface AuthService {
	LoginResponseDto login(LoginRequestDto loginRequestDto);
}
