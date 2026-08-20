package com.example.seunggu.notification.adapter.out.persistence;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 알림 아카이브(콜드 스토리지). 보존 기간이 지난 종결 상태 알림을 이곳으로 이관한다.
 */
@Entity
@Table(name = "notification_archive")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationArchiveJpaEntity {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, length = 200)
    private String recipient;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** 발송 이력 — 이관 시에도 보존해야 사후 원인 분석이 가능하다. */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "last_error_code", length = 50)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    /** 아카이브로 이관된 시각 (운영 추적용). */
    @Column(name = "archived_at", nullable = false, updatable = false)
    private LocalDateTime archivedAt;

    Notification toDomain() {
        return Notification.restore(id, idempotencyKey, channel, recipient, title, message,
                status, createdAt, sentAt,
                attemptCount, lastAttemptAt, lastErrorCode, lastErrorMessage);
    }
}
