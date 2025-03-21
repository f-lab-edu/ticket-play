package com.flab.tiple.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @MappedSuperclass: 이 클래스가 엔티티는 아니지만 자식 클래스에 매핑 정보를 상속해주는 부모 클래스임을 명시
 * @EntityListeners(AuditingEntityListener.class): JPA 엔티티의 이벤트(생성, 수정 등)를 감지하는 리스너를 등록
 * @CreatedDate: 엔티티가 생성될 때 시간을 자동으로 저장합니다.
 * @LastModifiedDate: 엔티티가 수정될 때 시간을 자동으로 업데이트합니다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTime {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public void update() {
        this.updatedAt = LocalDateTime.now();
    }

    // 논리적 삭제를 위한 메서드
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

}