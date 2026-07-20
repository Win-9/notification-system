package com.example.seunggu.notification.domain;

import java.util.UUID;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 알림 도메인 모델. 프레임워크(JPA/스프링)에 의존하지 않는 순수 자바 객체.
 * 영속 표현은 adapter.out.persistence 의 NotificationJpaEntity 가 담당한다.
 *
 * <p>상태 전이 규칙은 {@link NotificationStatus} 문서 참고 — 허용되지 않는 전이는
 * {@link IllegalStateException} 을 던져 도메인 수준에서 차단한다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notification {

    /** 알림의 고유 식별자. 저장 전에는 null. */
    private UUID id;

    /** 멱등성 키. 같은 요청을 식별해 중복 발송을 막는 값. */
    private final String idempotencyKey;

    /** 발송 채널. KAKAO / EMAIL / SMS. */
    private final NotificationChannel channel;

    /** 채널에 따라 전화번호·이메일 주소 등. */
    private final String recipient;

    /** 알림 제목 (선택). */
    private final String title;

    /** 알림 본문 내용. */
    private final String message;

    /** 처리 상태. 전이는 반드시 mark* 메서드를 통해서만 일어난다. */
    private NotificationStatus status;

    /** 접수(생성) 시각. */
    private final LocalDateTime createdAt;

    /** 발송 완료 시각. SENT 가 아닌 상태에서는 null. */
    private LocalDateTime sentAt;

    /** 신규 알림 생성. 상태는 PENDING 으로 시작한다. */
    public static Notification create(String idempotencyKey, NotificationChannel channel,
                                      String recipient, String title, String message) {
        return new Notification(null, idempotencyKey, channel, recipient, title, message,
                NotificationStatus.PENDING, LocalDateTime.now(), null);
    }

    /** 저장소에서 읽은 데이터를 도메인 객체로 복원한다 (영속성 어댑터 전용). */
    public static Notification restore(UUID id, String idempotencyKey, NotificationChannel channel,
                                       String recipient, String title, String message,
                                       NotificationStatus status, LocalDateTime createdAt, LocalDateTime sentAt) {
        return new Notification(id, idempotencyKey, channel, recipient, title, message,
                status, createdAt, sentAt);
    }

    /**
     * Consumer 가 발송 처리를 시작했다. (PENDING/RETRY_WAIT → PROCESSING)
     * PROCESSING 재진입 허용: 처리 중 크래시 후 Kafka 재전달 시 같은 상태에서 다시 시작할 수 있다.
     */
    public void markProcessing() {
        transition(NotificationStatus.PROCESSING,
                NotificationStatus.PENDING, NotificationStatus.RETRY_WAIT, NotificationStatus.PROCESSING);
    }

    /** 발송 성공 — 종결 상태. 반드시 PROCESSING 을 거쳐서만 도달한다. */
    public void markSent() {
        transition(NotificationStatus.SENT, NotificationStatus.PROCESSING);
        this.sentAt = LocalDateTime.now();
    }

    /** 발송 일시 실패 — 재시도 대기. */
    public void markRetryWait() {
        transition(NotificationStatus.RETRY_WAIT,
                NotificationStatus.PENDING, NotificationStatus.PROCESSING);
    }

    /** 재시도 소진 — 실패 확정 기록 (DLT). */
    public void markFailed() {
        transition(NotificationStatus.FAILED,
                NotificationStatus.PENDING, NotificationStatus.PROCESSING, NotificationStatus.RETRY_WAIT);
    }

    /** 자동 재시도 중단·격리 — 종결 상태. 운영자 확인 필요. */
    public void markDead() {
        transition(NotificationStatus.DEAD,
                NotificationStatus.PENDING, NotificationStatus.PROCESSING,
                NotificationStatus.RETRY_WAIT, NotificationStatus.FAILED);
    }

    /** 현재 상태가 허용 목록에 있을 때만 전이하고, 아니면 도메인 규칙 위반으로 거부한다. */
    private void transition(NotificationStatus target, NotificationStatus... allowedFrom) {
        for (NotificationStatus allowed : allowedFrom) {
            if (this.status == allowed) {
                this.status = target;
                return;
            }
        }
        throw new IllegalStateException(
                "허용되지 않는 상태 전이: %s → %s (알림 id=%s)".formatted(status, target, id));
    }
}
