package com.smartheritage.content.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.content.dto.*;
import com.smartheritage.content.events.ArtifactChanges;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArtifactDetailCacheTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final Map<String, String> store = new ConcurrentHashMap<>();
    private ArtifactDetailCache cache;
    private final AtomicInteger reads = new AtomicInteger();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> store.get(call.getArgument(0)));
        when(redis.delete(anyString())).thenAnswer(call -> store.remove(call.getArgument(0)) != null);
        doAnswer(call -> {
            RedisScript<?> script = call.getArgument(0);
            List<String> keys = call.getArgument(1);
            if (script.getScriptAsString().contains("INCR")) {
                store.put(keys.get(0), Long.toString(Long.parseLong(store.getOrDefault(keys.get(0), "0")) + 1));
                store.remove(keys.get(1)); return 1L;
            }
            if (!store.getOrDefault(keys.get(0), "0").equals(call.getArgument(2))) return 0L;
            store.put(keys.get(1), call.getArgument(3)); return 1L;
        }).when(redis).execute(any(RedisScript.class), anyList(), any(Object[].class));
        cache = new ArtifactDetailCache(redis,
                new ObjectMapper().findAndRegisterModules().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES),
                new SimpleMeterRegistry(), true, 120, "sh:content:detail:v1");
    }

    private ArtifactResponse load() {
        reads.incrementAndGet();
        return ArtifactResponse.builder().id(1L).siteId(1L).name("Original")
                .contents(List.of(ArtifactContentResponse.builder().languageCode("vi").title("Original").build()))
                .media(List.of()).build();
    }

    @Test
    void cacheHitReturnsIndependentDtoAndChangeInvalidates() {
        cache.get(1L, this::load).setName("English");
        assertThat(cache.get(1L, this::load).getName()).isEqualTo("Original");
        assertThat(reads.get()).isEqualTo(1);
        cache.changed(new ArtifactChanges.Changed(1L));
        cache.get(1L, this::load);
        assertThat(reads.get()).isEqualTo(2);
    }

    @Test
    void deletionDuringLoadCannotRefillOldValue() {
        cache.get(1L, () -> {
            ArtifactResponse stale = load();
            cache.changed(new ArtifactChanges.Changed(1L));
            return stale;
        });
        assertThat(store).doesNotContainKey(cache.dataKey(1L));
    }

    @Test
    void redisDownFallsBackAndDoesNotCacheDatabaseErrors() {
        when(redis.opsForValue()).thenThrow(new IllegalStateException("Redis down"));
        assertThat(cache.get(1L, this::load).getName()).isEqualTo("Original");
        assertThat(cache.get(1L, this::load).getName()).isEqualTo("Original");
        verify(redis, times(1)).opsForValue();
        assertThatThrownBy(() -> cache.get(2L, () -> { throw new IllegalArgumentException("not found"); }))
                .hasMessage("not found");
    }

    @Test
    void invalidCachedJsonReloadsSafely() {
        store.put(cache.dataKey(1L), "invalid-json");
        assertThat(cache.get(1L, this::load).getName()).isEqualTo("Original");
    }

    @Test
    void incompleteCachedSchemaReloadsInsteadOfFailingApi() {
        store.put(cache.dataKey(1L), "{}");
        assertThat(cache.get(1L, this::load).getId()).isEqualTo(1L);
        assertThat(reads.get()).isEqualTo(1);
    }

    @Test
    void concurrentMissesOnlyLoadOnce() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> calls = java.util.stream.IntStream.range(0, 16)
                    .mapToObj(i -> (Callable<String>) () -> cache.get(1L, this::load).getName()).toList();
            for (Future<String> result : workers.invokeAll(calls)) assertThat(result.get()).isEqualTo("Original");
            assertThat(reads.get()).isEqualTo(1);
        } finally { workers.shutdownNow(); }
    }
}
