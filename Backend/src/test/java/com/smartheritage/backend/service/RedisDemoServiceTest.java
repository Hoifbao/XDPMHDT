package com.smartheritage.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisDemoServiceTest {
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> values;
    @InjectMocks RedisDemoService redisDemoService;

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForValue()).thenReturn(values);
    }

    @Test
    void writesNamespacedRedisKeyWithTtl() {
        redisDemoService.write("hello", "Smart Heritage", Duration.ofMinutes(10));
        verify(values).set("smartheritage:personalization-demo:hello", "Smart Heritage", Duration.ofMinutes(10));
    }
}
