package com.flab.tiple.waiting.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.flab.tiple.global.message.TipleRedisKey;
import com.flab.tiple.member.exception.MemberNotMatchException;
import com.flab.tiple.member.repository.MemberRepository;
import com.flab.tiple.ticket.waiting.domain.TicketWaitingRedis;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingCancelResponseDto;
import com.flab.tiple.ticket.waiting.dto.response.TicketWaitingInfoResponseDto;
import com.flab.tiple.ticket.waiting.enums.TicketWaitingStatus;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingNotCancelableException;
import com.flab.tiple.ticket.waiting.exception.TicketWaitingNotFoundException;
import com.flab.tiple.ticket.waiting.repository.TicketWaitingRedisRepository;
import com.flab.tiple.ticket.waiting.service.TicketWaitingServiceImpl;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class TicketWaitingServiceTest {

	@InjectMocks
	private TicketWaitingServiceImpl ticketWaitingService;

	// Redis 관련 의존성
	@Mock
	private TicketWaitingRedisRepository ticketWaitingRedisRepository;

	// 테스트 환경에서 사용할 모킹된 빈 주입
	@Mock
	private RedisTemplate<String, Object> redisTemplate;

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private MemberRepository memberRepository;

	// 테스트 데이터
	private TicketWaitingRedis ticketWaitingRedis;
	private TicketWaitingRedis ticketWaitingRedis2;

	// TipleRedisKey.TICKET_WAITING_KEY.getKey()의 실제 값
	private static final String WAITING_KEY_PREFIX = TipleRedisKey.TICKET_RESERVATION_KEY.getKey();

	private Long waitingId = 1L;
	private Long concertId1 = 1L;
	private Long concertId2 = 2L;
	private Long memberId = 1L;

	@BeforeEach
	void setUp() {
		// Redis 웨이팅 객체 - 첫 번째 콘서트에 대한 대기
		ticketWaitingRedis = TicketWaitingRedis.builder()
			.concertId(concertId1)
			.memberId(memberId)
			.waitingNumber(5)
			.status(TicketWaitingStatus.WAITING.toString())
			.build();

		// 다른 콘서트에 대한 Redis 웨이팅 객체
		ticketWaitingRedis2 = TicketWaitingRedis.builder()
			.concertId(concertId2)
			.memberId(memberId)
			.waitingNumber(1)
			.status(TicketWaitingStatus.WAITING.toString())
			.build();
	}

	@Test
	@DisplayName("Redis에서 waiting 취소 성공")
	void cancelWaiting_Success() {
		// Given
		when(ticketWaitingRedisRepository.findById(WAITING_KEY_PREFIX + waitingId))
			.thenReturn(Optional.of(ticketWaitingRedis));
		when(ticketWaitingRedisRepository.save(any(TicketWaitingRedis.class)))
			.thenReturn(ticketWaitingRedis);

		// When
		TicketWaitingCancelResponseDto result = ticketWaitingService.cancelWaiting(waitingId, memberId);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(waitingId);
		assertThat(result.getStatus()).isEqualTo(TicketWaitingStatus.CANCELED);

		// 검증: Redis 저장 호출 확인 및 상태 변경 확인
		verify(ticketWaitingRedisRepository).save(argThat(waiting ->
			waiting.getStatus().equals(TicketWaitingStatus.CANCELED.toString())
		));
	}

	@Test
	@DisplayName("Redis에서 대기 정보를 찾을 수 없을 때")
	void cancelWaiting_WaitingNotFound() {
		// Given
		when(ticketWaitingRedisRepository.findById(WAITING_KEY_PREFIX + waitingId))
			.thenReturn(Optional.empty());

		// When & Then
		assertThrows(TicketWaitingNotFoundException.class, () ->
			ticketWaitingService.cancelWaiting(waitingId, memberId)
		);

		// 검증: Redis 조회만 호출되고 저장은 안 됨
		verify(ticketWaitingRedisRepository).findById(WAITING_KEY_PREFIX + waitingId);
		verify(ticketWaitingRedisRepository, never()).save(any());
	}

	@Test
	@DisplayName("Redis에서 waiting이 이미 취소된 상태일 때")
	void cancelWaiting_AlreadyCanceled() {
		// Given
		// 이미 취소된 상태의 대기 객체
		TicketWaitingRedis canceledWaiting = TicketWaitingRedis.builder()
			.concertId(concertId1)
			.memberId(memberId)
			.waitingNumber(5)
			.status(TicketWaitingStatus.CANCELED.toString())
			.build();

		when(ticketWaitingRedisRepository.findById(WAITING_KEY_PREFIX + waitingId))
			.thenReturn(Optional.of(canceledWaiting));

		// When & Then
		assertThrows(TicketWaitingNotCancelableException.class, () ->
			ticketWaitingService.cancelWaiting(waitingId, memberId)
		);

		// 검증: Redis 조회만 호출되고 저장은 안 됨
		verify(ticketWaitingRedisRepository).findById(WAITING_KEY_PREFIX + waitingId);
		verify(ticketWaitingRedisRepository, never()).save(any());
	}

	@Test
	@DisplayName("요청한 유저와 대기 정보의 유저가 일치하지 않을 때")
	void cancelWaiting_MemberNotMatch() {
		// Given
		Long otherMemberId = 2L;

		// 다른 사용자의 대기 객체
		TicketWaitingRedis otherMemberWaiting = TicketWaitingRedis.builder()
			.concertId(concertId1)
			.memberId(otherMemberId) // 다른 사용자 ID
			.waitingNumber(5)
			.status(TicketWaitingStatus.WAITING.toString())
			.build();

		when(ticketWaitingRedisRepository.findById(WAITING_KEY_PREFIX + waitingId))
			.thenReturn(Optional.of(otherMemberWaiting));

		// When & Then
		assertThrows(MemberNotMatchException.class, () ->
			ticketWaitingService.cancelWaiting(waitingId, memberId)
		);

		// 검증: Redis 조회만 호출되고 저장은 안 됨
		verify(ticketWaitingRedisRepository).findById(WAITING_KEY_PREFIX + waitingId);
		verify(ticketWaitingRedisRepository, never()).save(any());
	}

	@Test
	@DisplayName("회원의 대기 목록 조회 - 여러 건 정렬 확인")
	void getMemberWaitingList_SortedByWaitingNumber() {
		// Given
		// Redis 키 패턴 조회 결과
		Set<String> redisKeys = new HashSet<>();
		// 키 패턴에 맞는 형식의 키 생성
		String key1 = WAITING_KEY_PREFIX + concertId1 + ":" + memberId;
		String key2 = WAITING_KEY_PREFIX + concertId2 + ":" + memberId;
		redisKeys.add(key1);
		redisKeys.add(key2);

		// 패턴으로 키 조회 모킹 - Mockito.any() 사용
		when(redisTemplate.keys(any(String.class))).thenReturn(redisKeys);

		// 각 키에 대한 Redis 객체 조회 결과 모킹
		when(ticketWaitingRedisRepository.findById(key1))
			.thenReturn(Optional.of(ticketWaitingRedis));  // 대기번호 5
		when(ticketWaitingRedisRepository.findById(key2))
			.thenReturn(Optional.of(ticketWaitingRedis2)); // 대기번호 1

		// When
		List<TicketWaitingInfoResponseDto> result = ticketWaitingService.getMemberWaitingList(memberId);

		// Then
		assertThat(result.size()).isEqualTo(2);

		// 검증: 대기번호 순으로 정렬됐는지 확인 (1, 5)
		assertThat(result.get(0).getWaitingNumber()).isEqualTo(1);
		assertThat(result.get(1).getWaitingNumber()).isEqualTo(5);

		// Redis 조회 확인 - any() 매처로 확인
		verify(redisTemplate).keys(any(String.class));
		verify(ticketWaitingRedisRepository).findById(key1);
		verify(ticketWaitingRedisRepository).findById(key2);
	}

	@Test
	@DisplayName("회원의 대기 목록 조회 - 대기 없는 경우")
	void getMemberWaitingList_Empty() {
		// Given
		// Redis 키 패턴 조회 결과 없음 - any() 매처 사용
		when(redisTemplate.keys(any(String.class))).thenReturn(Collections.emptySet());

		// When
		List<TicketWaitingInfoResponseDto> result = ticketWaitingService.getMemberWaitingList(memberId);

		// Then
		assertThat(result.size()).isEqualTo(0);

	}

}