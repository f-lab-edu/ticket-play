package com.flab.tiple.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginRequestDto {
	@NotNull
	@Email
	private String email;
	@NotNull
	private String password;

	@Builder
	public LoginRequestDto(String email, String password) {
		this.email = email;
		this.password = password;
	}
}