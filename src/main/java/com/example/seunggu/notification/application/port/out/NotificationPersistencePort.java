package com.example.seunggu.notification.application.port.out;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.seunggu.notification.domain.Notification;

/**
 * 아웃바운드 포트: 알림 영속화.
 * 구현은 adapter.out.persistence (JPA/MySQL) 가 담당한다.
 */
public interface NotificationPersistencePort {

    /**
     * 저장(신규/갱신). idempotency_key 유니크 제약 위반 시
     * DataIntegrityViolationException 이 전파되며, 중복 여부 판정은 호출부가 조회로 확인한다.
     */
    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    Optional<Notification> findByIdIncludeArchive(UUID id);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    /**
     * 수신자별 since 이후 알림을 최신순으로 offset 페이징 조회. page 는 0부터.
     */
    List<Notification> findByRecipient(String recipient, LocalDateTime since, int page, int size);

    /**
     * 수신자별 since 이후 알림을 최신순으로 커서 페이징 조회.
     *
     * @param cursor null 이면 첫 페이지, 값이 있으면 그보다 오래된 것부터
     * @param limit  읽을 최대 건수 (호출부가 hasNext 판별을 위해 size+1 을 넘길 수 있다)
     */
    List<Notification> findByRecipientWithCursor(String recipient, LocalDateTime since, UUID cursor, int limit);

    boolean claimForProcessing(UUID notificationId, Duration lease);
}
