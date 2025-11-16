package com.ragchat.chat.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Value("${rate-limit.capacity}")
    private int capacity;

    @Value("${rate-limit.refill-tokens}")
    private int refillTokens;

    @Value("${rate-limit.refill-duration-minutes}")
    private int refillDuration;

    @Bean
    public Map<String, Bucket> rateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }

    public Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillTokens, Duration.ofMinutes(refillDuration))
                .build();

        return Bucket.builder().addLimit(limit).build();
    }
}
