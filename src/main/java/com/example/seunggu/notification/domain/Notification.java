package com.example.seunggu.notification.domain;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 엔티티. 상태를 DB(MySQL)에 저장한다.
 */
@Entity
@Table(name = "notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_idempotency_key",
                columnNames = "idempotency_key"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    /** PK. 알림의 고유 식별자 (자동 증가). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 멱등성 키. 같은 요청을 식별해 중복 발송을 막는 값 (유니크 제약). */
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    /** 발송 채널. KAKAO / EMAIL / SMS 중 하나 (문자열로 저장). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    /** 채널에 따라 전화번호·이메일 주소 등 */
    @Column(nullable = false, length = 200)
    private String recipient;

    /** 알림 제목 (선택). */
    @Column(length = 200)
    private String title;

    /** 알림 본문 내용. */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** 처리 상태. PENDING(접수) → SENT(발송 완료) / FAILED(발송 실패). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    /** 접수(생성) 시각. **/
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 발송 완료 시각. 발송 전(PENDING/FAILED)에는 null. */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public Notification(String idempotencyKey,
                        NotificationChannel channel,
                        String recipient,
                        String title,
                        String message) {
        this.idempotencyKey = idempotencyKey;
        this.channel = channel;
        this.recipient = recipient;
        this.title = title;
        this.message = message;
        this.status = NotificationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
    }
}
