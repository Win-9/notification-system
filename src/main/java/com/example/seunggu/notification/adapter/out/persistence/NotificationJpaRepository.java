package com.example.seunggu.notification.adapter.out.persistence;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 아카이브로 이관 완료된 행만 핫 테이블에서 제거한다.
     * EXISTS 조건으로 "아카이브에 실제로 존재함"을 확인하므로,
     * 복사가 누락된 행이 삭제되는 일이 구조적으로 불가능하다.
     */
    @Modifying
    @Query(value = """
            DELETE FROM notification
            WHERE created_at < :threshold
              AND status IN ('SENT', 'FAILED', 'DEAD')
              AND EXISTS (SELECT 1 FROM notification_archive a WHERE a.id = notification.id)
            LIMIT :chunkSize
            """, nativeQuery = true)
    int deleteArchivedChunk(@Param("threshold") LocalDateTime threshold, @Param("chunkSize") int chunkSize);

    long countByCreatedAtBefore(LocalDateTime threshold);

    @Modifying
    @Query(value = """
        UPDATE notification
        SET status = 'PROCESSING',
            attempt_count = attempt_count + 1,
            last_attempt_at = :now
        WHERE id = :id
          AND ( status IN ('PENDING', 'RETRY_WAIT')
                OR (status = 'PROCESSING' AND last_attempt_at < :leaseExpiry) )
        """, nativeQuery = true)
    int claimForProcessing(@Param("id") UUID id, @Param("now") LocalDateTime now, @Param("leaseExpiry") LocalDateTime minus);
}
