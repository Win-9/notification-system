package com.example.seunggu.notification.service;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.dto.NotificationRequest;
import com.example.seunggu.notification.dto.NotificationResponse;
import com.example.seunggu.notification.event.NotificationRegisteredEvent;
import com.example.seunggu.notification.repository.NotificationRepository;
import java.time.Duration;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 등록 시 알림을 저장(PENDING)하고, 커밋 이후 비동기 발송을 위해 이벤트를 발행.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final RedissonClient redissonClient;
    private final NotificationRegistrar registrar;

    private static final String IDEM_PREFIX = "noti:idem:";
    private static final Duration IDEM_TTL = Duration.ofHours(24);

    public NotificationResponse register(String key, NotificationRequest request) {
        validate(request);

        // SETNX + TTL
        RBucket<String> marker = redissonClient.getBucket(IDEM_PREFIX + key);
        boolean first = marker.setIfAbsent("PROCESSING", IDEM_TTL);
        if (!first) {
            // 이미 처리됐거나 처리 중 → 중복
            throw new DuplicateRequestException("이미 처리된(또는 처리 중인) 요청입니다: " + key);
        }

        try {
            // save 트랜잭션 분리
            return registrar.persist(key, request);
        } catch (DataIntegrityViolationException e) {
            // Redis 마커는 유실됐지만 DB 엔 이미 존재
            log.info("DB 유니크 제약으로 중복 방지 (Redis 마커 유실 추정) key={}", key);
            throw new DuplicateRequestException("이미 처리된 요청입니다: " + key);
        } catch (RuntimeException e) {
            // 처리 실패 → Redis 마커를 지워 재시도를 허용한다.
            marker.delete();
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<NotificationResponse> find(Long id) {
        return repository.findById(id).map(NotificationResponse::from);
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
}
