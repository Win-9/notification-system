package com.example.seunggu.notification.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    Optional<NotificationJpaEntity> findByIdempotencyKey(String idempotencyKey);

    /** offset 페이징용 */
    List<NotificationJpaEntity> findByRecipientAndCreatedAtAfterOrderByCreatedAtDesc(
            String recipient, LocalDateTime createdAt, Pageable pageable);

    /**
     * 커서 페이징 — 첫 페이지 (커서 없음).
     */
    List<NotificationJpaEntity> findByRecipientAndCreatedAtAfterOrderByIdDesc(
            String recipient, LocalDateTime createdAt, Pageable pageable);

    /**
     * 커서 페이징 — 커서 이후 (id 가 커서보다 작은 = 더 오래된 것).
     */
    List<NotificationJpaEntity> findByRecipientAndCreatedAtAfterAndIdLessThanOrderByIdDesc(
            String recipient, LocalDateTime createdAt, UUID cursor, Pageable pageable);
}
