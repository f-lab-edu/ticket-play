package com.flab.tiple.member.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flab.tiple.global.entity.BaseTime;
import com.flab.tiple.member.enums.RoleEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    // JSON 직렬화 시 패스워드 필드를 제외하기 위해 @JsonIgnore사용.
    @JsonIgnore
    private String password;
    private String name;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleEnum role;


    @Builder
    public Member(String name, String email, String password, RoleEnum role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // 패스워드가 로그 등에 노출되지 않도록 하기 위해 toString() 메서드를 오버라이드
    @Override
    public String toString() {
        return "Member{" +
            "id=" + id +
            ", email='" + email + '\'' +
            '}';
    }
}
