package com.example.seunggu.notification.domain;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 알림 도메인 모델. 프레임워크(JPA/스프링)에 의존하지 않는 순수 자바 객체.
 * 영속 표현은 adapter.out.persistence 의 NotificationJpaEntity 가 담당한다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notification {

    /** 알림의 고유 식별자. 저장 전에는 null. */
    private Long id;

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

    /** 처리 상태. PENDING(접수) → SENT(발송 완료) / FAILED(발송 실패). */
    private NotificationStatus status;

    /** 접수(생성) 시각. */
    private final LocalDateTime createdAt;

    /** 발송 완료 시각. 발송 전(PENDING/FAILED)에는 null. */
    private LocalDateTime sentAt;

    /** 신규 알림 생성. 상태는 PENDING 으로 시작한다. */
    public static Notification create(String idempotencyKey, NotificationChannel channel,
                                      String recipient, String title, String message) {
        return new Notification(null, idempotencyKey, channel, recipient, title, message,
                NotificationStatus.PENDING, LocalDateTime.now(), null);
    }

    /** 저장소에서 읽은 데이터를 도메인 객체로 복원한다 (영속성 어댑터 전용). */
    public static Notification restore(Long id, String idempotencyKey, NotificationChannel channel,
                                       String recipient, String title, String message,
                                       NotificationStatus status, LocalDateTime createdAt, LocalDateTime sentAt) {
        return new Notification(id, idempotencyKey, channel, recipient, title, message,
                status, createdAt, sentAt);
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
    }
}
