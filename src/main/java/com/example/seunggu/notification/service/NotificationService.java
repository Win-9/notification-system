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
 * 알림 등록/조회 서비스.
 * 등록 시 알림을 저장(PENDING)하고, 커밋 이후 비동기 발송을 위해 이벤트를 발행한다.
 * 실제 발송은 {@link NotificationDispatcher} 가 thread pool 에서 처리한다.
 *
 * <p>멱등성: Redis 를 <b>판별 주체</b>로 삼는다.
 * {@code RBucket.setIfAbsent}(= SETNX + TTL)는 원자적이라 "처음인지" 판별과
 * 동시 요청 차단을 한 번에 처리한다. DB 의 유니크 제약은 Redis 유실 시 대비한 최후 방어선이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final RedissonClient redissonClient;

    private static final String IDEM_PREFIX = "noti:idem:";
    private static final Duration IDEM_TTL = Duration.ofHours(24);

    @Transactional
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
            // Redis 마커는 유실됐지만 DB 엔 이미 존재 → 유니크 제약이 잡아준 경우 (최후 방어선).
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
