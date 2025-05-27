package com.flab.tiple.security;

import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.flab.tiple.exception.ErrorCode;
import com.flab.tiple.exception.TokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.expiration-ms}")
	private long expirationMs;

	/**
	 * @PostConstruct: 빈 초기화시 자동 실행
	 * Base64:  이진데이터를 텍스트로 변환해주는 인코딩 방법
	 *
	 * 비밀키를 Base64로 인코딩하는 이유
	 * 		1. Base32와의 비교: 는 32개 문자 (A-Z, 2-7)만 지원 이 때 소문자를 지원하지 않고 숫자도 부분적으로 지원됨.
	 * 	    2. 문자 호환성: 비밀키에 특수문자나 비 ASCII 문자가 포함될 수 있는데, 이런 문자들은 시스템마다 다르게 해석될 수 있습니다. Base64로 인코딩하면 모든 시스템에서 동일하게 해석되는 ASCII 문자만 사용.
	 */
	@PostConstruct
	protected void init() {
		secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
	}

	/**
	 * 토큰 생성 ( 이메일, GrantedAuthority를 상속받은 권한들) -> 단순히 String으로 이루어져 있음
	 *  1. 현재 날짜와 만료 날짜 계산후
	 *  2. 토큰 생성
	 */
	public String generateToken(String email, Long memberId,Collection<? extends GrantedAuthority> authorities) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
			.setSubject(email) // 제목등록
			.claim("memberId", memberId) // memberId 추가
			.claim("roles", authorities.stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList())) // 클레임 등록
			.setIssuedAt(now) // 서명일
			.setExpiration(expiryDate) //종료일
			.signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256) //sign하는 키와 서명 알고리즘
			.compact();
	}
	/**
	 * 토큰에서 이메일 및 권한 정보 가져오기
	 *  1. setSigningKey을 통해 secretKey의 바이트코드를 가져와 키생성
	 *  2. parseClaimsJws를 통해 JWT(헤더,페이로드, 서명)을 분리 후 계산된 서명과 JWT서명을 비교.유효하면 Jwt<Claims>반환
	 */
	private Claims parseToken(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	public String getEmailFromToken(String token) {
		Claims claims = parseToken(token);
		return claims.getSubject();
	}

	public List<String> getRolesFromToken(String token) {
		Claims claims = parseToken(token);
		return claims.get("roles", List.class);
	}
	public Long getMemberIdFromToken(String token) {
		// 토큰에서 회원 ID 추출하는 로직
		// 예: Claims에서 memberId 클레임 추출
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
			.build()
			.parseClaimsJws(token)
			.getBody();

		return claims.get("memberId", Long.class);
	}
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder()
				.setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
				.build()
				.parseClaimsJws(token);
			return true;
		} catch (ExpiredJwtException e) {
			log.error("만료된 JWT 토큰입니다.");
			throw new TokenException(ErrorCode.TOKEN_EXPIRED, "만료된 JWT 토큰입니다.");
		} catch (JwtException e) {
			log.error("유효하지 않은 JWT 토큰입니다.");
			throw new TokenException(ErrorCode.TOKEN_INVALID, "유효하지 않은 JWT 토큰입니다.");
		} catch (IllegalArgumentException e) {
			log.error("JWT 토큰이 비어있거나 올바르지 않습니다.");
			throw new TokenException(ErrorCode.TOKEN_MALFORMED, "JWT 토큰이 올바르지 않습니다.");
		} catch (Exception e) {
			log.error("그 외 JWT토큰 에러입니다.");
			throw new TokenException(ErrorCode.TOKEN_ERROR, "JWT 토큰 관련 에러가 발생했습니다.");
		}
	}
}