package com.flab.tiple.application.dto.request;


import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberCreateRequestDto {

  //  @NotNull
    private String name;

   // @NotNull
   // @Email
    private String email;

    //@NotNull
    private String password;

    @Builder
    public MemberCreateRequestDto(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

}
