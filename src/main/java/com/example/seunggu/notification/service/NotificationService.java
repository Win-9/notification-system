package com.example.seunggu.notification.service;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.dto.NotificationRequest;
import com.example.seunggu.notification.dto.NotificationResponse;
import com.example.seunggu.notification.event.NotificationRegisteredEvent;
import com.example.seunggu.notification.repository.NotificationRepository;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 등록/조회 서비스.
 * 등록 시 알림을 저장(PENDING)하고, 커밋 이후 비동기 발송을 위해 이벤트를 발행한다.
 * 실제 발송은 {@link NotificationDispatcher} 가 thread pool 에서 처리한다.
 *
 * <p>멱등성(패턴 ②): Redis 분산 락(RLock)으로 동시 요청을 1차 차단하고,
 * DB 의 idempotency_key 유니크 제약을 최후 방어선으로 둔다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "noti:idem:lock:";

    @Transactional
    public NotificationResponse register(String key, NotificationRequest request) {
        validate(request);

        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        boolean locked;
        try {
            locked = lock.tryLock(0, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("락 획득 중 인터럽트 발생", e);
        }

        if (!locked) {
            throw new DuplicateRequestException("이미 처리 중인 요청입니다: " + key);
        }

        try {
            Optional<Notification> existing = repository.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                log.info("멱등 요청 감지 — 기존 알림 반환 key={}", key);
                return NotificationResponse.from(existing.get());
            }

            Notification saved = repository.save(new Notification(
                    key,
                    request.getChannel(),
                    request.getRecipient(),
                    request.getTitle(),
                    request.getMessage()
            ));
            // 커밋 이후 발송이 트리거되도록 이벤트 발행 (AFTER_COMMIT 리스너).
            eventPublisher.publishEvent(new NotificationRegisteredEvent(saved.getId()));
            return NotificationResponse.from(saved);

        } catch (DataIntegrityViolationException e) {
            log.info("동시 멱등 경합 — 유니크 제약으로 중복 저장 방지 key={}", key);
            throw new DuplicateRequestException("이미 처리된 요청입니다: " + key);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
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
