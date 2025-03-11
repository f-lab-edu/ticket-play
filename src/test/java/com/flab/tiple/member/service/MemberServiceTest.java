package com.flab.tiple.member.service;

import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.dto.request.MemberCreateRequestDto;
import com.flab.tiple.member.dto.response.MemberCreateResponseDto;
import com.flab.tiple.member.exception.MemberDuplicatedException;
import com.flab.tiple.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordUtil;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    @DisplayName("회원가입 성공")
    void signUpSuccess() {
        // Given
        MemberCreateRequestDto requestDto = MemberCreateRequestDto.builder()
                .email("test@gmail.com")
                .password("passwordTest133")
                .name("test")
                .build();

        Member mockMember = Member.builder()
                .email(requestDto.getEmail())
                .name(requestDto.getName())
                .password("encryptedPassword")
                .build();

        when(memberRepository.existsByEmail(requestDto.getEmail())).thenReturn(false);
        when(passwordUtil.encode(requestDto.getPassword())).thenReturn("encryptedPassword");
        when(memberRepository.save(any(Member.class))).thenReturn(mockMember);

        // When
        MemberCreateResponseDto responseDto = memberService.signUp(requestDto);

        // Then
        assertNotNull(responseDto);
        assertEquals(requestDto.getEmail(), responseDto.getEmail());
        verify(memberRepository).save(any());
    }


    @Test
    @DisplayName("회원가입 실패 : 이메일 중복")
    void signUpFail() {
        // Given
        MemberCreateRequestDto requestDto = MemberCreateRequestDto.builder()
                .email("test@gmail.com")
                .password("passwordTest133")
                .name("test")
                .build();

        when(memberRepository.existsByEmail(requestDto.getEmail())).thenReturn(true); // 이메일이 중복됨

        // When & Then
        assertThrows(MemberDuplicatedException.class, () -> memberService.signUp(requestDto));
    }

}
