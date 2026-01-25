package com.c2c.csm.adapter.out.presence;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.c2c.csm.application.port.out.presenece.SessionPresencePort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisSessionPresenceAdapter implements SessionPresencePort {
    private static final String KEY_PREFIX = "presence:session:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public String getRoutingKeyByUserId(String userId) {
        return redisTemplate.opsForValue().get(key(userId));
    }

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }
}
