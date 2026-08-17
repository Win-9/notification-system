package com.example.seunggu.notification.adapter.out.redis;

import com.example.seunggu.notification.application.port.out.IdempotencyPort;
import java.time.Duration;

import io.lettuce.core.RedisException;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        try {
            return bucket(key).setIfAbsent("PROCESSING", ttl);
        } catch (RedisException e) {
            log.warn("멱등성 점령 실패 -> DB 제약으로 이동 - key = {}", key);
            return true;
        }
    }

    @Override
    public void release(String key) {
        bucket(key).delete();
    }

    private RBucket<String> bucket(String key) {
        return redissonClient.getBucket(keyPrefix + key);
    }
}
