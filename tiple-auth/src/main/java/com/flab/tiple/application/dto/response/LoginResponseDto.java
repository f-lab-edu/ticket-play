package com.flab.tiple.application.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponseDto {
	private String token;


	public LoginResponseDto(String token) {
		this.token = token;
	}
}
