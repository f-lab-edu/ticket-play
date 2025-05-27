package com.flab.tiple.presentation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flab.tiple.application.dto.request.LoginRequestDto;
import com.flab.tiple.application.dto.response.LoginResponseDto;
import com.flab.tiple.application.service.AuthServiceImpl;
import com.flab.tiple.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthServiceImpl authService;

	@PostMapping("/login")
	public ApiResponse<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
		LoginResponseDto responseDto = authService.login(loginRequestDto);
		return ApiResponse.success(responseDto);
	}

}
