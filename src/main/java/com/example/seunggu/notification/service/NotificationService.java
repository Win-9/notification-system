package com.example.seunggu.notification.service;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.dto.IdempotencyDto;
import com.example.seunggu.notification.dto.NotificationRequest;
import com.example.seunggu.notification.dto.NotificationResponse;
import com.example.seunggu.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 등록/조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationRegistrar registrar;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "noti:idem:";
    private static final Duration PENDING_TTL = Duration.ofSeconds(10);
    private static final Duration DONE_TTL = Duration.ofHours(24);

    public NotificationResponse register(String key, NotificationRequest request) {
        validate(request);

        RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + key, StringCodec.INSTANCE);

        boolean claimed = bucket.setIfAbsent(write(IdempotencyDto.pending()), PENDING_TTL);
        if (!claimed) {
            return handleExisting(key, bucket);
        }

        try {
            NotificationResponse response = registrar.persist(key, request);

            bucket.set(write(IdempotencyDto.done(response)), DONE_TTL);
            return response;

        } catch (DataIntegrityViolationException e) {
            NotificationResponse existing = repository.findByIdempotencyKey(key)
                    .map(NotificationResponse::from)
                    .orElseThrow(() -> new DuplicateRequestException("이미 처리된 요청입니다: " + key));
            bucket.set(write(IdempotencyDto.done(existing)), DONE_TTL);
            return existing;

        } catch (RuntimeException e) {
            bucket.delete();
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<NotificationResponse> find(Long id) {
        return repository.findById(id).map(NotificationResponse::from);
    }

    private NotificationResponse handleExisting(String key, RBucket<String> bucket) {
        String raw = bucket.get();
        if (raw == null) {
            throw new DuplicateRequestException("요청을 처리 중입니다. 잠시 후 다시 시도하세요: " + key);
        }

        IdempotencyDto record = read(raw);
        if (record.isPending()) {
            throw new DuplicateRequestException("이미 처리 중인 요청입니다: " + key);
        }
        return record.toResponse();
    }

    private void validate(NotificationRequest request) {
        if (request.getChannel() == null) {
            throw new IllegalArgumentException("channel 은 필수입니다. (KAKAO/EMAIL/SMS)");
        }
        if (request.getRecipient() == null || request.getRecipient().isBlank()) {
            throw new IllegalArgumentException("recipient 는 필수입니다.");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("message 는 필수입니다.");
        }
    }

    private String write(IdempotencyDto record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("멱등성 레코드 직렬화 실패", e);
        }
    }

    private IdempotencyDto read(String raw) {
        try {
            return objectMapper.readValue(raw, IdempotencyDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("멱등성 레코드 역직렬화 실패", e);
        }
    }
}
