package com.flab.tiple.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flab.tiple.application.dto.request.MemberCreateRequestDto;
import com.flab.tiple.application.dto.response.MemberCreateResponseDto;
import com.flab.tiple.application.service.MemberService;
import com.flab.tiple.response.ApiResponse;

import lombok.AllArgsConstructor;

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
