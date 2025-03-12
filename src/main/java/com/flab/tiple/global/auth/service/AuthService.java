package com.flab.tiple.global.auth.service;

import com.flab.tiple.global.auth.dto.request.LoginRequestDto;
import com.flab.tiple.global.auth.dto.response.LoginResponseDto;

public interface AuthService {
	LoginResponseDto login(LoginRequestDto loginRequestDto);
}
