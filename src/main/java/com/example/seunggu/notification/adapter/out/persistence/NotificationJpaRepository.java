package com.example.seunggu.notification.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    Optional<NotificationJpaEntity> findByIdempotencyKey(String idempotencyKey);

    List<NotificationJpaEntity> findByRecipientAndCreatedAtAfterOrderByCreatedAtDesc(
            String recipient, LocalDateTime createdAt, Pageable pageable);
}
