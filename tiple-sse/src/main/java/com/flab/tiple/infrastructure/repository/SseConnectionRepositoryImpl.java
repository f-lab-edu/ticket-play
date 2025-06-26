package com.flab.tiple.infrastructure.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.flab.tiple.domain.model.SseConnection;
import com.flab.tiple.domain.repository.SseConnectionRepository;

@Repository
public class SseConnectionRepositoryImpl implements SseConnectionRepository {
	private final Map<String, SseConnection> connections = new ConcurrentHashMap<>();

	@Override
	public void save(SseConnection connection) {
		connections.put(connection.getConnectionId(), connection);
	}

	@Override
	public void remove(String connectionId) {
		connections.remove(connectionId);
	}

	@Override
	public Optional<SseConnection> findById(String connectionId) {
		return Optional.ofNullable(connections.get(connectionId));
	}

	@Override
	public List<SseConnection> findByMemberId(Long memberId) {
		return connections.values().stream()
			.filter(connection -> connection.belongsToMember(memberId))
			.collect(Collectors.toList());
	}

	@Override
	public int countAll() {
		return connections.size();
	}
}