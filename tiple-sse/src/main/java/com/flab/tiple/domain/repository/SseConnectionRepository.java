package com.flab.tiple.domain.repository;

import java.util.List;
import java.util.Optional;

import com.flab.tiple.domain.model.SseConnection;

public interface SseConnectionRepository {
	void save(SseConnection connection);
	void remove(String connectionId);
	Optional<SseConnection> findById(String connectionId);
	List<SseConnection> findByMemberId(Long memberId);
	int countAll();
}