package com.ragchat.storage.service;

import com.ragchat.storage.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.rate-limit.requests}")
    private int maxRequests;

    @Value("${app.rate-limit.duration-seconds}")
    private int durationSeconds;

    public void checkRateLimit(String userId) {
        String key = "rate_limit:" + userId;

        String currentCount = redisTemplate.opsForValue().get(key);

        if (currentCount == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(durationSeconds));
            log.debug("Rate limit initialized for user: {}", userId);
        } else {
            int count = Integer.parseInt(currentCount);

            if (count >= maxRequests) {
                log.warn("Rate limit exceeded for user: {}", userId);
                throw new RateLimitExceededException(
                    String.format("Rate limit exceeded. Maximum %d requests per %d seconds allowed.",
                                  maxRequests, durationSeconds));
            }

            redisTemplate.opsForValue().increment(key);
            log.debug("Rate limit count for user {}: {}", userId, count + 1);
        }
    }
}