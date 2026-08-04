package com.example.seunggu.notification.adapter.out.persistence;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * 알림의 JPA 영속 표현. 도메인 모델(Notification)과 분리되어
 * JPA 어노테이션·DB 스키마 관심사를 어댑터 안에 가둔다.
 */
@Entity
@Table(name = "notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_idempotency_key",
                columnNames = "idempotency_key"),
                indexes = @Index(name = "idx_recipient_created", columnList = "recipient, created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationJpaEntity {

    @Id
    @UuidGenerator(algorithm = Uuid7Generator.class)
    private UUID id;

    /** 멱등성 키 (유니크 제약 — 중복 방지 최후 방어선). */
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

    /** 발송 시도 횟수 (PROCESSING 전이마다 증가). */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    /** 마지막 시도 시각. */
    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    /** 마지막 실패 분류 코드 (HTTP_500, TIMEOUT, CIRCUIT_OPEN 등). */
    @Column(name = "last_error_code", length = 50)
    private String lastErrorCode;

    /** 마지막 실패 상세 메시지. */
    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    static NotificationJpaEntity fromDomain(Notification notification) {
        return new NotificationJpaEntity(
                notification.getId(),
                notification.getIdempotencyKey(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getSentAt(),
                notification.getAttemptCount(),
                notification.getLastAttemptAt(),
                notification.getLastErrorCode(),
                notification.getLastErrorMessage()
        );
    }

    Notification toDomain() {
        return Notification.restore(id, idempotencyKey, channel, recipient, title, message,
                status, createdAt, sentAt,
                attemptCount, lastAttemptAt, lastErrorCode, lastErrorMessage);
    }
}
