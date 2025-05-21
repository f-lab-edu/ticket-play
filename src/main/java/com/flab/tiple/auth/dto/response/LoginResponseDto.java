package com.flab.tiple.auth.dto.response;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponseDto {
	private String token;

	@Builder
	public LoginResponseDto(String token) {
		this.token = token;
	}
}
