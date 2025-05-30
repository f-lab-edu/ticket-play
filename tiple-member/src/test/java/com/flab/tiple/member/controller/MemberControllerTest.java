package com.flab.tiple.member.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.tiple.api.MemberController;
import com.flab.tiple.application.dto.request.MemberCreateRequestDto;
import com.flab.tiple.application.dto.response.MemberCreateResponseDto;
import com.flab.tiple.application.service.MemberService;
import com.flab.tiple.config.AbstractRestDocs;
import com.flab.tiple.response.ApiResponse;
import com.flab.tiple.security.CustomUserDetailsService;
import com.flab.tiple.security.JwtTokenProvider;
import com.flab.tiple.security.SecurityConfig;
import com.flab.tiple.util.JwtTokenUtil;

@WebMvcTest({MemberController.class, SecurityConfig.class})
public class MemberControllerTest extends AbstractRestDocs {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    // SecurityConfig에 필요한 의존성 MockBean으로 추가
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("회원가입")
    @Test
    void signupSuccess() throws Exception {
        // Given
        MemberCreateRequestDto requestDto = MemberCreateRequestDto.builder().
                name("test").
                email("test@gmail.com").
                password("testPassword").
                build();

        MemberCreateResponseDto responseDto = MemberCreateResponseDto.builder().
                id(1L).
                email("test@gmail.com").
                build();

        given(memberService.signUp(any(MemberCreateRequestDto.class)))
                .willReturn(responseDto);

        //when
        ResultActions resultActions = mockMvc.perform(
                        post("/api/members/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)));




        //then
        MockHttpServletResponse response = resultActions.andExpect(status().isOk())
                .andReturn().getResponse();


        ApiResponse<MemberCreateResponseDto> apiResponse = objectMapper.readValue(
                response.getContentAsString(),
                new TypeReference<ApiResponse<MemberCreateResponseDto>>() {}
        );

        Assertions.assertThat(apiResponse.getStatus()).isEqualTo(200);
        Assertions.assertThat(apiResponse.getMessage()).isEqualTo("Success");
        Assertions.assertThat(apiResponse.getData()).usingRecursiveComparison().isEqualTo(responseDto);

    }

}
