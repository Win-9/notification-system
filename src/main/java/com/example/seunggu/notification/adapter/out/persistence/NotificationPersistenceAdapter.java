package com.example.seunggu.notification.adapter.out.persistence;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.domain.Notification;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            return repository.save(NotificationJpaEntity.fromDomain(notification)).toDomain();
        } catch (DataIntegrityViolationException e) {
            // idempotency_key 유니크 제약 위반 — 기술 예외를 도메인 의미로 번역해 올린다.
            throw new DuplicateRequestException(
                    "이미 처리된 요청입니다: " + notification.getIdempotencyKey());
        }
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return repository.findById(id).map(NotificationJpaEntity::toDomain);
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(NotificationJpaEntity::toDomain);
    }
}
