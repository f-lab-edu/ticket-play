package com.flab.tiple.application.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberCreateResponseDto {
    private Long id;
    private String email;

    @Builder
    public MemberCreateResponseDto(Long id, String email) {
        this.id = id;
        this.email = email;
    }

}
