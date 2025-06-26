package com.flab.tiple.member.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.flab.tiple.application.dto.request.MemberCreateRequestDto;
import com.flab.tiple.application.dto.response.MemberCreateResponseDto;
import com.flab.tiple.application.service.MemberServiceImpl;
import com.flab.tiple.domain.exception.MemberDuplicatedException;
import com.flab.tiple.entity.member.Member;
import com.flab.tiple.repository.MemberRepository;

/**
 * Import에서 *는 피하는게 좋은 이유
 *   1. 이름 충돌 위험: 여러 패키지에서 동일한 이름의 클래스를 와일드카드로 임포트할 경우, 예상치 못한 이름 충돌이 발생할 수 있음.
 *   2. 의도하지 않은 사용: 패키지 전체를 임포트하면 실제로 필요하지 않은 클래스까지 접근 가능해져, 의도하지 않은 사용이 발생할 수 있음.
 */

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
