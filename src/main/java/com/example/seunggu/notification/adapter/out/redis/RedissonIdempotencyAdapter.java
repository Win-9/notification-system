package com.example.seunggu.notification.adapter.out.redis;

import com.example.seunggu.notification.application.port.out.IdempotencyPort;
import java.time.Duration;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 어댑터(Redis). SETNX + TTL 로 멱등성 키를 원자적으로 선점한다.
 * 실제 명령: SET {key-prefix}{key} "PROCESSING" NX PX {ttl}
 * 키 프리픽스·TTL 은 application.properties 의 notification.idem.* 로 설정한다.
 */
@Component
public class RedissonIdempotencyAdapter implements IdempotencyPort {

    private final RedissonClient redissonClient;
    private final String keyPrefix;
    private final Duration ttl;

    public RedissonIdempotencyAdapter(RedissonClient redissonClient,
                                      @Value("${notification.idem.key-prefix}") String keyPrefix,
                                      @Value("${notification.idem.ttl}") Duration ttl) {
        this.redissonClient = redissonClient;
        this.keyPrefix = keyPrefix;
        this.ttl = ttl;
    }

    @Override
    public boolean tryClaim(String key) {
        return bucket(key).setIfAbsent("PROCESSING", ttl);
    }

    @Override
    public void release(String key) {
        bucket(key).delete();
    }

    private RBucket<String> bucket(String key) {
        return redissonClient.getBucket(keyPrefix + key);
    }
}
