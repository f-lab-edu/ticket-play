package com.flab.tiple.application.service;


import org.springframework.transaction.annotation.Transactional;

import com.flab.tiple.application.dto.request.MemberCreateRequestDto;
import com.flab.tiple.application.dto.response.MemberCreateResponseDto;
import com.flab.tiple.entity.member.Member;
import com.flab.tiple.entity.member.enums.RoleEnum;
import com.flab.tiple.exception.ErrorCode;
import com.flab.tiple.exception.MemberDuplicatedException;
import com.flab.tiple.repository.MemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.AllArgsConstructor;


@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 멤버 존재여부 체크와 생성로직을 두개의 메서드로 분리했을 때 장점
     * 1. 단일 책임 원칙(SRP) 준수: 각 메서드가 단 하나의 책임만 가지게 됨. isExistCheckByEmail은 이메일 중복 체크만, signUp 메서드는 회원 생성만 담당.
     * 2. 코드 재사용성 향상: 이메일 중복 체크 로직이 필요한 다른 기능(예: 프로필 업데이트, 이메일 변경 등)에서도 isExistCheckByEmail 메서드를 재사용할 수 있음.
     * 3. 테스트 용이성: 각 메서드를 독립적으로 테스트할 수 있어 단위 테스트가 더 명확하고 간결해짐.
     */
    @Transactional
    @Override
    public MemberCreateResponseDto signUp(MemberCreateRequestDto memberCreateRequestDto) {
        isExistCheckByEmail(memberCreateRequestDto.getEmail());

        String encryptedPassword = passwordEncoder.encode(memberCreateRequestDto.getPassword());

        Member newMember = Member.builder()
                .email(memberCreateRequestDto.getEmail())
                .name(memberCreateRequestDto.getName())
                .password(encryptedPassword)
                .role(RoleEnum.ROLE_USER)
                .build();

        Member member= memberRepository.save(newMember);

        return MemberCreateResponseDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .build();
    }
    public void isExistCheckByEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new MemberDuplicatedException(ErrorCode.MEMBER_DUPLICATED,ErrorCode.MEMBER_DUPLICATED.getDescription());
        }
    }
}
