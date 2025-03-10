package com.flab.tiple.member.service;

import com.flab.tiple.global.exception.ErrorCode;
import com.flab.tiple.global.util.PasswordUtil;
import com.flab.tiple.member.domain.Member;
import com.flab.tiple.member.dto.request.MemberCreateRequestDto;
import com.flab.tiple.member.dto.response.MemberCreateResponseDto;
import com.flab.tiple.member.exception.MemberDuplicatedException;
import com.flab.tiple.member.repository.MemberRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordUtil passwordUtil;

    @Transactional
    @Override
    public MemberCreateResponseDto signUp(MemberCreateRequestDto memberCreateRequestDto) {
        if (memberRepository.existsByEmail(memberCreateRequestDto.getEmail())) {
            throw new MemberDuplicatedException(ErrorCode.MEMBER_DUPLICATED,ErrorCode.MEMBER_DUPLICATED.getDescription());
        }

        String encryptedPassword = passwordUtil.encrypt(memberCreateRequestDto.getPassword());

        Member newMember = Member.builder()
                .email(memberCreateRequestDto.getEmail())
                .name(memberCreateRequestDto.getName())
                .password(encryptedPassword)
                .build();

        Member member= memberRepository.save(newMember);

        return MemberCreateResponseDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .build();
    }
}
