package com.flab.tiple.member.service;

import com.flab.tiple.member.dto.request.MemberCreateRequestDto;
import com.flab.tiple.member.dto.response.MemberCreateResponseDto;

public interface MemberService {
    MemberCreateResponseDto signUp(MemberCreateRequestDto memberCreateRequestDto);
}
