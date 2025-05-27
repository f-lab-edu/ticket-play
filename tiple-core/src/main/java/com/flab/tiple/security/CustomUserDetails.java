package com.flab.tiple.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.flab.tiple.entity.member.Member;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CustomUserDetails 구현 방식의 장점
 * Member 객체 전체를 갖고 있어 이메일, 이름 등 다양한 사용자 정보에 접근할 수 있음.
 * 여기서는 UserDetails객체 내부에 대해 좀 더 자세히 조사하고자 작성.
 */
@Getter
@AllArgsConstructor
public class CustomUserDetails  implements UserDetails {
	private final Member member;

	/**
	 * GrantedAuthority 객체
	 *   스프링 시큐리티에서 인가(Authorization) 과정에 사용
	 *   권한 체크에 사용되며 SecurityConfig에서 .hasRole("USER") 등의 설정과 일치하는지 확인.
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.singletonList(
			new SimpleGrantedAuthority(member.getRole().name())
		);
	}

	@Override
	public String getPassword() {
		return member.getPassword();
	}

	@Override
	public String getUsername() {
		return member.getEmail();
	}

	/**
	 * isAccountNonExpired : 계정이 만료되지 않았는지 여부
	 * false 반환 시: "계정이 만료되었습니다" 오류로 로그인 거부(default값이 true임)
	 */
	@Override
	public boolean isAccountNonExpired() {
		// 만료 로직이 필요하면 여기서 구현
		return UserDetails.super.isAccountNonExpired();
	}

	/**
	 * isAccountNonLocked: 계정이 잠겨있지 않은지 여부
	 * false 반환 시: "계정이 잠겼습니다" 오류로 로그인 거부(default값이 true임)
	 */
	@Override
	public boolean isAccountNonLocked() {
		return UserDetails.super.isAccountNonLocked();
	}

	/**
	 * isCredentialsNonExpired : 자격 증명(비밀번호)이 만료되지 않았는지 여부
	 * false 반환 시: "비밀번호가 만료되었습니다" 오류로 로그인 거부(default값이 true임)
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		return UserDetails.super.isCredentialsNonExpired();
	}

	/**
	 * isEnabled : 계정이 활성화되어 있는지 여부
	 * false 반환 시: "계정이 비활성화되었습니다" 오류로 로그인 거부(default값이 true임)
	 */
	@Override
	public boolean isEnabled() {
		return UserDetails.super.isEnabled();
	}
}
