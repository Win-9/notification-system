package com.example.seunggu.notification.adapter.out.persistence;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.domain.Notification;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 어댑터(JPA/MySQL). 도메인 모델 ↔ JPA 엔티티 매핑을 담당한다.
 */
@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationPersistencePort {

    private final NotificationJpaRepository repository;

    @Override
    public Notification save(Notification notification) {
        return repository.save(NotificationJpaEntity.fromDomain(notification)).toDomain();
    }

    @Override
    public boolean claimForProcessing(UUID notificationId, Duration lease) {
        LocalDateTime now = LocalDateTime.now();
        return repository.claimForProcessing(notificationId, now, now.minus(lease)) > 0;
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id).map(NotificationJpaEntity::toDomain);
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(NotificationJpaEntity::toDomain);
    }

    @Override
    public List<Notification> findByRecipient(String recipient, LocalDateTime since, int page, int size) {
        return repository
                .findByRecipientAndCreatedAtAfterOrderByCreatedAtDesc(recipient, since, PageRequest.of(page, size))
                .stream()
                .map(NotificationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Notification> findByRecipientWithCursor(String recipient, LocalDateTime since, UUID cursor, int limit) {
        PageRequest limitOnly = PageRequest.of(0, limit);
        List<NotificationJpaEntity> rows = (cursor == null)
                ? repository.findByRecipientAndCreatedAtAfterOrderByIdDesc(recipient, since, limitOnly)
                : repository.findByRecipientAndCreatedAtAfterAndIdLessThanOrderByIdDesc(recipient, since, cursor, limitOnly);
        return rows.stream().map(NotificationJpaEntity::toDomain).toList();
    }
}
