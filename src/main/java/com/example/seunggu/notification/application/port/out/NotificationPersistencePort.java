package com.example.seunggu.notification.application.port.out;

import com.example.seunggu.notification.domain.Notification;
import java.util.Optional;

/**
 * 아웃바운드 포트: 알림 영속화.
 * 구현은 adapter.out.persistence (JPA/MySQL) 가 담당한다.
 */
public interface NotificationPersistencePort {

    /**
     * 저장(신규/갱신). idempotency_key 유니크 제약 위반 시
     * DuplicateRequestException 으로 번역되어 올라온다 (멱등성 최후 방어선).
     */
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);
}
