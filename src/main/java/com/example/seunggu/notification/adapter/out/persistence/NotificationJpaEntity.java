package com.example.seunggu.notification.adapter.out.persistence;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 알림의 JPA 영속 표현. 도메인 모델(Notification)과 분리되어
 * JPA 어노테이션·DB 스키마 관심사를 어댑터 안에 가둔다.
 */
@Entity
@Table(name = "notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_idempotency_key",
                columnNames = "idempotency_key"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationJpaEntity {

    /** PK. 알림의 고유 식별자 (자동 증가). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
                notification.getSentAt()
        );
    }

    Notification toDomain() {
        return Notification.restore(id, idempotencyKey, channel, recipient, title, message,
                status, createdAt, sentAt);
    }
}
