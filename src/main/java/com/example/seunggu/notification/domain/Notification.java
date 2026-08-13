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
 *
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notification {

    /** 에러 메시지 보관 상한 — 컬럼 길이를 넘지 않도록 저장 시 자른다. */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

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

    /** 발송 시도 횟수. PROCESSING 전이(= 시도 시작)마다 1 증가한다. SENT 인 경우 "몇 번째 시도에 성공했는지"를 뜻한다. */
    private int attemptCount;

    /** 마지막 시도 시각. 이 값 + attemptCount + backoff 정책으로 다음 시도 시점을 추정할 수 있다. */
    private LocalDateTime lastAttemptAt;

    /** 마지막 실패 분류 코드 (예: HTTP_500, TIMEOUT, CIRCUIT_OPEN). 성공 시 초기화하지 않고 이력으로 남긴다. */
    private String lastErrorCode;

    /** 마지막 실패 상세 메시지 (상한 500자). */
    private String lastErrorMessage;

    /** 신규 알림 생성. 상태는 PENDING 으로 시작한다. */
    public static Notification create(String idempotencyKey, NotificationChannel channel,
                                      String recipient, String title, String message) {
        return new Notification(null, idempotencyKey, channel, recipient, title, message,
                NotificationStatus.PENDING, LocalDateTime.now(), null,
                0, null, null, null);
    }

    /** 저장소에서 읽은 데이터를 도메인 객체로 복원한다 (영속성 어댑터 전용). */
    public static Notification restore(UUID id, String idempotencyKey, NotificationChannel channel,
                                       String recipient, String title, String message,
                                       NotificationStatus status, LocalDateTime createdAt, LocalDateTime sentAt,
                                       int attemptCount, LocalDateTime lastAttemptAt,
                                       String lastErrorCode, String lastErrorMessage) {
        return new Notification(id, idempotencyKey, channel, recipient, title, message,
                status, createdAt, sentAt,
                attemptCount, lastAttemptAt, lastErrorCode, lastErrorMessage);
    }

    /**
     * Consumer 가 발송 처리를 시작했다. (PENDING/RETRY_WAIT → PROCESSING)
     * PROCESSING 재진입 허용: 처리 중 크래시 후 Kafka 재전달 시 같은 상태에서 다시 시작할 수 있다.
     *
     * <p>시도 횟수는 성공·실패와 무관하게 <b>시도 시작 시점</b>에 증가시킨다 —
     * 그래야 SENT 도 "몇 번째 시도에 성공했는지"를 남기고, 크래시로 결과 기록이 누락된 시도도 집계된다.
     */
    public void markProcessing() {
        transition(NotificationStatus.PROCESSING,
                NotificationStatus.PENDING, NotificationStatus.RETRY_WAIT, NotificationStatus.PROCESSING);
        this.attemptCount++;
        this.lastAttemptAt = LocalDateTime.now();
    }

    /** 발송 성공 — 종결 상태. 반드시 PROCESSING 을 거쳐서만 도달한다. */
    public void markSent() {
        transition(NotificationStatus.SENT, NotificationStatus.PROCESSING);
        this.sentAt = LocalDateTime.now();
    }

    /** 발송 일시 실패 — 재시도 대기. 실패 원인을 기록한다. */
    public void markRetryWait(String errorCode, String errorMessage) {
        transition(NotificationStatus.RETRY_WAIT,
                NotificationStatus.PENDING, NotificationStatus.PROCESSING);
        recordError(errorCode, errorMessage);
    }

    /** 재시도 소진 — 실패 확정 기록 (DLT). */
    public void markFailed(String errorCode, String errorMessage) {
        transition(NotificationStatus.FAILED,
                NotificationStatus.PENDING, NotificationStatus.PROCESSING, NotificationStatus.RETRY_WAIT);
        recordError(errorCode, errorMessage);
    }

    /** 자동 재시도 중단·격리 — 종결 상태. 운영자 확인 필요. */
    public void markDead(String errorCode, String errorMessage) {
        transition(NotificationStatus.DEAD,
                NotificationStatus.PENDING, NotificationStatus.PROCESSING,
                NotificationStatus.RETRY_WAIT, NotificationStatus.FAILED);
        recordError(errorCode, errorMessage);
    }

    private void recordError(String errorCode, String errorMessage) {
        this.lastErrorCode = errorCode;
        this.lastErrorMessage = truncate(errorMessage);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
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
