package com.flab.tiple.application.service;

import com.flab.tiple.application.dto.request.MemberCreateRequestDto;
import com.flab.tiple.application.dto.response.MemberCreateResponseDto;

public interface MemberService {
    MemberCreateResponseDto signUp(MemberCreateRequestDto memberCreateRequestDto);
}
