package com.smartheritage.content.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.content.dto.ArtifactResponse;
import com.smartheritage.content.events.ArtifactChanges;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Cache detached DTOs. Lua version guard prevents an old in-flight read refilling an evicted key. */
@Component
public class ArtifactDetailCache {
    private static final Logger log = LoggerFactory.getLogger(ArtifactDetailCache.class);
    private static final DefaultRedisScript<Long> SAVE = new DefaultRedisScript<>("""
            if (redis.call('GET', KEYS[1]) or '0') ~= ARGV[1] then return 0 end
            redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[3]); return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> INVALIDATE = new DefaultRedisScript<>("""
            redis.call('INCR', KEYS[1]); redis.call('PEXPIRE', KEYS[1], ARGV[1]);
            redis.call('DEL', KEYS[2]); return 1
            """, Long.class);
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final MeterRegistry meters;
    private final boolean enabled;
    private final Duration ttl;
    private final String namespace;
    private final Object[] locks = new Object[64];
    private final Set<Long> dirty = ConcurrentHashMap.newKeySet();
    private volatile long retryAfter;

    public ArtifactDetailCache(StringRedisTemplate redis, ObjectMapper json, MeterRegistry meters,
            @Value("${app.detail-cache.enabled:true}") boolean enabled,
            @Value("${app.detail-cache.ttl-seconds:120}") long ttlSeconds,
            @Value("${app.detail-cache.namespace:sh:content:detail:v1}") String namespace) {
        this.redis = redis; this.json = json; this.meters = meters; this.enabled = enabled;
        if (ttlSeconds < 1) throw new IllegalArgumentException("Cache TTL must be positive");
        ttl = Duration.ofSeconds(ttlSeconds);
        this.namespace = namespace;
        Arrays.setAll(locks, i -> new Object());
    }

    public ArtifactResponse get(Long id, Supplier<ArtifactResponse> load) {
        if (!enabled || System.nanoTime() < retryAfter) return load.get();
        synchronized (locks[Math.floorMod(id.hashCode(), locks.length)]) {
            String version;
            try {
                if (dirty.contains(id)) { evict(id); dirty.remove(id); }
                version = Objects.requireNonNullElse(redis.opsForValue().get(versionKey(id)), "0");
                String cached = redis.opsForValue().get(dataKey(id));
                if (cached != null) {
                    try {
                        ArtifactResponse response = json.readValue(cached, ArtifactResponse.class);
                        if (!Objects.equals(id, response.getId()) || response.getContents() == null || response.getMedia() == null) {
                            throw new IllegalArgumentException("Invalid cached artifact schema");
                        }
                        count("hit"); return response;
                    } catch (Exception corrupt) {
                        redis.delete(dataKey(id));
                    }
                }
            } catch (RuntimeException unavailable) {
                unavailable(); return load.get();
            }
            count("miss");
            ArtifactResponse response = load.get(); // Propagate real 404/DB errors; never cache errors.
            try {
                redis.execute(SAVE, List.of(versionKey(id), dataKey(id)), version,
                        json.writeValueAsString(response), Long.toString(ttl.toMillis()));
            } catch (Exception unavailable) { unavailable(); }
            return response;
        }
    }

    @Order(-10)
    @TransactionalEventListener
    public void changed(ArtifactChanges.Changed change) {
        if (!enabled) return;
        dirty.add(change.artifactId());
        try { evict(change.artifactId()); dirty.remove(change.artifactId()); }
        catch (RuntimeException unavailable) { unavailable(); }
    }

    private void evict(Long id) {
        redis.execute(INVALIDATE, List.of(versionKey(id), dataKey(id)), Long.toString(ttl.multipliedBy(2).toMillis()));
    }
    public String dataKey(Long id) { return namespace + ":{" + id + "}:data"; }
    private String versionKey(Long id) { return namespace + ":{" + id + "}:version"; }
    private void count(String result) { meters.counter("sh.content.detail.cache", "result", result).increment(); }
    private void unavailable() {
        long now = System.nanoTime();
        if (now >= retryAfter) log.warn("Detail cache unavailable; serving from database");
        retryAfter = now + Duration.ofSeconds(2).toNanos();
        count("unavailable");
    }
}
