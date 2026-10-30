package com.smartheritage.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/** Small Redis cache-aside demo; all demo keys are namespaced. */
@Service
@RequiredArgsConstructor
public class RedisDemoService {
    private static final String PREFIX = "smartheritage:personalization-demo:";
    private final StringRedisTemplate redisTemplate;

    public void write(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(redisKey(key), value, ttl);
    }

    public Optional<String> read(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(key)));
    }

    public boolean delete(String key) {
        Boolean deleted = redisTemplate.delete(redisKey(key));
        return Boolean.TRUE.equals(deleted);
    }

    private String redisKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9:_-]{1,64}")) {
            throw new IllegalArgumentException("Redis key chỉ dùng A-Z, a-z, 0-9, :, _ hoặc -, tối đa 64 ký tự.");
        }
        return PREFIX + key;
    }
}
