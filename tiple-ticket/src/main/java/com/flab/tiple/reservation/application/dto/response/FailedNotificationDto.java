package com.flab.tiple.reservation.application.dto.response;


import com.flab.tiple.reservation.application.notification.enums.NotificationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedNotificationDto {
	private String id;                    // 고유 ID (UUID)
	private String type;                  // 알림 타입 (reservation, waiting, seat)
	private Long memberId;                // 회원 ID
	private String originalEventData;     // 원본 이벤트 JSON
	private int attemptCount;             // 재시도 횟수
	private long createdAt;               // 생성 시간
	private long nextRetryAt;             // 다음 재시도 시간
	private NotificationStatus status;                // 상태 (PENDING, RETRYING, FAILED, SUCCESS)

	public void increaseAttemptCount() {
		this.attemptCount = this.attemptCount+1;
	}

	public void changeRetryStatus(){
		this.status = NotificationStatus.RETRYING;
	}

	public void changeNextRetryAt(long nextRetryAt){
		this.nextRetryAt = nextRetryAt;
	}
}
