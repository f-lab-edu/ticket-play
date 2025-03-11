package com.flab.tiple.member.controller;

import com.flab.tiple.global.response.ApiResponse;
import com.flab.tiple.member.dto.request.MemberCreateRequestDto;
import com.flab.tiple.member.dto.response.MemberCreateResponseDto;
import com.flab.tiple.member.service.MemberService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/signup")
    public ApiResponse<MemberCreateResponseDto> signup(@RequestBody MemberCreateRequestDto memberCreateRequestDto) {
       MemberCreateResponseDto response = memberService.signUp(memberCreateRequestDto);
       return ApiResponse.success(response);
    }

}
