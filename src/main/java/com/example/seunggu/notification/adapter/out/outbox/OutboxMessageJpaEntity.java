package com.example.seunggu.notification.adapter.out.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Transactional Outbox 레코드.
 * "발행할 메시지"를 업무 데이터와 같은 트랜잭션으로 저장해,
 * 커밋과 Kafka 발행 사이의 크래시로 인한 메시지 유실을 구조적으로 차단한다.
 * 실제 발행은 {@link OutboxRelay} 가 폴링으로 수행한다 (at-least-once).
 */
@Entity
@Table(name = "outbox_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 발행 대상 토픽. */
    @Column(nullable = false, length = 200)
    private String topic;

    /** Kafka 메시지 key (파티션 배정용). */
    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    /** Kafka 메시지 value. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** 발행 완료 여부. false 인 행만 릴레이가 집어간다. */
    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private OutboxMessageJpaEntity(String topic, String messageKey, String payload) {
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.published = false;
        this.createdAt = LocalDateTime.now();
    }

    static OutboxMessageJpaEntity create(String topic, String messageKey, String payload) {
        return new OutboxMessageJpaEntity(topic, messageKey, payload);
    }

    void markPublished() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }
}
