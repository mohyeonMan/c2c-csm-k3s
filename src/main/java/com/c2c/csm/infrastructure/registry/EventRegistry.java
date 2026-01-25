package com.c2c.csm.infrastructure.registry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.c2c.csm.application.model.Event;
import com.c2c.csm.common.util.CommonMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventRegistry {
    private static final String EVENT_KEY_PREFIX = "c2c:event:registry:";
    private static final String RETRY_HASH_KEY = "c2c:event:registry:retry";
    private static final String PENDING_ZSET_KEY = "c2c:event:registry:pending";
    private static final long INITIAL_RETRY_DELAY_SECONDS = 5L;
    private static final long EVENT_TTL_BUFFER_SECONDS = 1L;
    private static final int MAX_BACKOFF_EXPONENT = 10;

    private static final DefaultRedisScript<Long> SAVE_SCRIPT = new DefaultRedisScript<>(
        "redis.call('SET', KEYS[1], ARGV[1]) " +
        "redis.call('EXPIRE', KEYS[1], ARGV[5]) " +
        "redis.call('HSET', KEYS[2], ARGV[2], ARGV[3]) " +
        "redis.call('ZADD', KEYS[3], ARGV[4], ARGV[2]) " +
        "return 1",
        Long.class
    );

    private static final DefaultRedisScript<Long> RESCHEDULE_SCRIPT = new DefaultRedisScript<>(
        "redis.call('EXPIRE', KEYS[1], ARGV[3]) " +
        "redis.call('HINCRBY', KEYS[2], ARGV[1], 1) " +
        "redis.call('ZADD', KEYS[3], ARGV[2], ARGV[1]) " +
        "return 1",
        Long.class
    );

    private static final DefaultRedisScript<Long> REMOVE_SCRIPT = new DefaultRedisScript<>(
        "redis.call('DEL', KEYS[1]) " +
        "redis.call('HDEL', KEYS[2], ARGV[1]) " +
        "redis.call('ZREM', KEYS[3], ARGV[1]) " +
        "return 1",
        Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final CommonMapper commonMapper;

    public void save(Event event) {
        save(event, null);
    }

    public void save(Event event, Instant nextAttemptAt) {
        if (event == null || event.getEventId() == null) {
            log.warn("Skip save: event or eventId is null");
            return;
        }
        String payload = commonMapper.write(event);
        if (payload == null) {
            log.warn("Skip save: failed to serialize event {}", event.getEventId());
            return;
        }
        String eventKey = eventKey(event.getEventId());
        Instant target = nextAttemptAt == null
            ? Instant.now().plusSeconds(INITIAL_RETRY_DELAY_SECONDS)
            : nextAttemptAt;
        long score = target.toEpochMilli();
        long ttlSeconds = calculateTtlSeconds(target);
        redisTemplate.execute(
            SAVE_SCRIPT,
            List.of(eventKey, RETRY_HASH_KEY, PENDING_ZSET_KEY),
            payload,
            event.getEventId(),
            "0",
            Long.toString(score),
            Long.toString(ttlSeconds)
        );
    }

    public void reschedule(String eventId, Instant nextAttemptAt) {
        if (eventId == null) {
            return;
        }
        long score = nextAttemptAt.toEpochMilli();
        long ttlSeconds = calculateTtlSeconds(nextAttemptAt);
        redisTemplate.execute(
            RESCHEDULE_SCRIPT,
            List.of(eventKey(eventId), RETRY_HASH_KEY, PENDING_ZSET_KEY),
            eventId,
            Long.toString(score),
            Long.toString(ttlSeconds)
        );
    }

    public void remove(String eventId) {
        if (eventId == null) {
            return;
        }
        redisTemplate.execute(
            REMOVE_SCRIPT,
            List.of(eventKey(eventId), RETRY_HASH_KEY, PENDING_ZSET_KEY),
            eventId
        );
    }

    public Optional<RegisteredEvent> get(String eventId) {
        if (eventId == null) {
            return Optional.empty();
        }
        String payload = redisTemplate.opsForValue().get(eventKey(eventId));
        if (payload == null) {
            return Optional.empty();
        }
        Event event = commonMapper.read(payload, Event.class);
        if (event == null) {
            return Optional.empty();
        }
        Long retryCount = readRetryCount(eventId);
        return Optional.of(new RegisteredEvent(event, retryCount));
    }

    public List<RegisteredEvent> findDue(Instant now, int batchSize) {
        if (batchSize <= 0) {
            return Collections.emptyList();
        }
        Set<String> eventIds = redisTemplate.opsForZSet()
            .rangeByScore(PENDING_ZSET_KEY, Double.NEGATIVE_INFINITY, now.toEpochMilli(), 0, batchSize);
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> idList = new ArrayList<>(eventIds);
        List<String> eventKeys = idList.stream().map(this::eventKey).toList();
        List<String> payloads = redisTemplate.opsForValue().multiGet(eventKeys);
        List<Object> retryCounts = redisTemplate.opsForHash().multiGet(RETRY_HASH_KEY, new ArrayList<>(idList));

        List<RegisteredEvent> results = new ArrayList<>(idList.size());
        for (int i = 0; i < idList.size(); i++) {
            String payload = payloads == null ? null : payloads.get(i);
            if (payload == null) {
                continue;
            }
            Event event = commonMapper.read(payload, Event.class);
            if (event == null) {
                continue;
            }
            long retry = 0L;
            if (retryCounts != null && retryCounts.size() > i && retryCounts.get(i) != null) {
                retry = Long.parseLong(retryCounts.get(i).toString());
            }
            results.add(new RegisteredEvent(event, retry));
        }
        return results;
    }

    public Instant calculateNextAttemptAt(long retryCount) {
        return calculateNextAttemptAt(retryCount, Instant.now());
    }

    public Instant calculateNextAttemptAt(long retryCount, Instant baseTime) {
        long normalized = Math.max(0, retryCount);
        int exponent = (int) Math.min(normalized, MAX_BACKOFF_EXPONENT);
        long delaySeconds = INITIAL_RETRY_DELAY_SECONDS * (1L << exponent);
        return baseTime.plusSeconds(delaySeconds);
    }

    private long calculateTtlSeconds(Instant nextAttemptAt) {
        long nowMillis = Instant.now().toEpochMilli();
        long deltaMillis = Math.max(0L, nextAttemptAt.toEpochMilli() - nowMillis);
        long delaySeconds = Math.max(1L, deltaMillis / 1000L);
        return (delaySeconds * 2L) + EVENT_TTL_BUFFER_SECONDS;
    }

    private Long readRetryCount(String eventId) {
        Object value = redisTemplate.opsForHash().get(RETRY_HASH_KEY, eventId);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String eventKey(String eventId) {
        return EVENT_KEY_PREFIX + eventId;
    }

    public record RegisteredEvent(Event event, long retryCount) {}
}
