package com.finvault.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
    private final StringRedisTemplate redisTemplate;
    private final Map<String, Instant> memoryBlacklist = new ConcurrentHashMap<>();

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(String jwtId, Instant expiresAt) {
        if (jwtId == null || expiresAt == null) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        memoryBlacklist.put(jwtId, expiresAt);
        try {
            redisTemplate.opsForValue().set(key(jwtId), "revoked", ttl);
        } catch (RuntimeException ignored) {
            memoryBlacklist.put(jwtId, expiresAt);
        }
    }

    public boolean isBlacklisted(String jwtId) {
        if (jwtId == null) {
            return false;
        }
        Instant expiry = memoryBlacklist.get(jwtId);
        if (expiry != null && expiry.isAfter(Instant.now())) {
            return true;
        }
        if (expiry != null) {
            memoryBlacklist.remove(jwtId);
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(jwtId)));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String key(String jwtId) {
        return "jwt:blacklist:" + jwtId;
    }
}

