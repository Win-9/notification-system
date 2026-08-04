package com.example.seunggu.notification.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationArchiveJpaRepository extends JpaRepository<NotificationArchiveJpaEntity, UUID> {

    /**
     * 보존 기간이 지난 종결 상태 알림을 아카이브로 복사한다 (INSERT ... SELECT).
     */
    @Modifying
    @Query(value = """
            INSERT INTO notification_archive
                (id, idempotency_key, channel, recipient, title, message, status, created_at, sent_at, archived_at)
            SELECT id, idempotency_key, channel, recipient, title, message, status, created_at, sent_at, NOW()
            FROM notification
            WHERE created_at < :threshold
              AND status IN ('SENT', 'FAILED', 'DEAD')
            ORDER BY created_at
            LIMIT :chunkSize
            """, nativeQuery = true)
    int copyChunkFromHot(@Param("threshold") LocalDateTime threshold, @Param("chunkSize") int chunkSize);
}
