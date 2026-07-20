package com.example.seunggu.notification.adapter.out.persistence;

import java.util.UUID;

import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.domain.Notification;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
    public Optional<Notification> findById(UUID id) {
        return repository.findById(id).map(NotificationJpaEntity::toDomain);
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(NotificationJpaEntity::toDomain);
    }
}
