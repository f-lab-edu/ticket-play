package com.flab.tiple.util;

import org.springframework.stereotype.Component;

import com.flab.tiple.messaging.redis.TipleRedisKey;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis 키 생성 및 파싱을 담당하는 유틸리티 클래스
 */
@Component
@Slf4j
public class RedisKeyUtils {

	private static final String WAITING_KEY_PREFIX = TipleRedisKey.TICKET_RESERVATION_KEY.getKey();
	private static final String CACHE_KEY_PREFIX = "cache:member:waiting:";
	private static final String LOCK_KEY_PREFIX = "lock:waiting:";

	// Sorted Set 관련 키 프리픽스 추가
	private static final String MEMBER_WAITING_SORTED_SET_PREFIX = "member:waiting:sorted:";

	private static final String KEY_SEPARATOR = ":";

	/**
	 * 회원별 대기 목록 캐시 키 생성
	 * @param memberId 회원 ID
	 * @return 캐시 키
	 */
	public String generateMemberWaitingCacheKey(Long memberId) {
		return CACHE_KEY_PREFIX + memberId;
	}

	/**
	 * 회원별 대기 목록 Sorted Set 키 생성
	 * @param memberId 회원 ID
	 * @return Sorted Set 키
	 */
	public String generateMemberWaitingSortedSetKey(Long memberId) {
		return MEMBER_WAITING_SORTED_SET_PREFIX + memberId;
	}

	/**
	 * 분산 락 키 생성
	 * @param resourceId 리소스 ID
	 * @return 락 키
	 */
	public String generateLockKey(String resourceId) {
		return LOCK_KEY_PREFIX + resourceId;
	}

	/**
	 * 대기 정보 Redis 키 생성
	 * @param waitingId 대기 ID
	 * @return Redis 키
	 */
	public String generateWaitingKey(Long waitingId) {
		return WAITING_KEY_PREFIX + waitingId;
	}

	/**
	 * 회원 패턴 키 생성 (KEYS 명령용 - 주의: 프로덕션에서는 SCAN 사용 권장)
	 * @param memberId 회원 ID
	 * @return 패턴 키
	 */
	public String generateMemberWaitingPattern(Long memberId) {
		return WAITING_KEY_PREFIX + "*:" + memberId;
	}

	/**
	 * Redis 키에서 대기 ID 추출
	 * @param redisKey Redis 키
	 * @return 대기 ID
	 * @throws IllegalArgumentException 키 형식이 올바르지 않은 경우
	 */
	public Long extractIdFromRedisKey(String redisKey) {
		try {
			if (redisKey == null || !redisKey.startsWith(WAITING_KEY_PREFIX)) {
				throw new IllegalArgumentException("유효하지 않은 Redis 키 형식: " + redisKey);
			}

			// 키에서 프리픽스 제거 후 첫 번째 부분을 ID로 추출
			String keyWithoutPrefix = redisKey.substring(WAITING_KEY_PREFIX.length());
			String[] parts = keyWithoutPrefix.split(KEY_SEPARATOR);

			if (parts.length == 0) {
				throw new IllegalArgumentException("키에서 ID를 추출할 수 없습니다: " + redisKey);
			}

			return Long.parseLong(parts[0]);
		} catch (NumberFormatException e) {
			log.error("Redis 키에서 ID 추출 실패: {}", redisKey, e);
			throw new IllegalArgumentException("키에서 숫자 ID를 추출할 수 없습니다: " + redisKey, e);
		}
	}

}