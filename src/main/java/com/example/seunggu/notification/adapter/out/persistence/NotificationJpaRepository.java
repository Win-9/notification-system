package com.example.seunggu.notification.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    Optional<NotificationJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
