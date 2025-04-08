package com.flab.tiple.member.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberInfoDto {
	private String email;
	private String name;

	@Builder
	public MemberInfoDto(String email, String name) {
		this.email = email;
		this.name = name;
	}
}
