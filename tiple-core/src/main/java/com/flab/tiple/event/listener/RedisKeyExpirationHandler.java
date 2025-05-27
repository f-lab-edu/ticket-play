package com.flab.tiple.event.listener;

public interface RedisKeyExpirationHandler {
	boolean canHandle(String key);
	void handle(String key);
}