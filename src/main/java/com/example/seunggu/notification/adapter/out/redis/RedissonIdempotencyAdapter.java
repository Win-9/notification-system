package com.example.seunggu.notification.adapter.out.redis;

import com.example.seunggu.notification.application.port.out.IdempotencyPort;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 어댑터(Redis). SETNX + TTL 로 멱등성 키를 원자적으로 선점한다.
 * 실제 명령: SET noti:idem:{key} "PROCESSING" NX PX {ttl}
 */
@Component
@RequiredArgsConstructor
public class RedissonIdempotencyAdapter implements IdempotencyPort {

    private final RedissonClient redissonClient;

    private static final String IDEM_PREFIX = "noti:idem:";
    private static final Duration IDEM_TTL = Duration.ofHours(24);

    @Override
    public boolean tryClaim(String key) {
        return bucket(key).setIfAbsent("PROCESSING", IDEM_TTL);
    }

    @Override
    public void release(String key) {
        bucket(key).delete();
    }

    private RBucket<String> bucket(String key) {
        return redissonClient.getBucket(IDEM_PREFIX + key);
    }
}
