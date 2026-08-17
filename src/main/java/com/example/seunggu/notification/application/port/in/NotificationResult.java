package com.example.seunggu.notification.application.port.in;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 유스케이스의 출력. 어댑터(웹 등)가 응답으로 변환해 쓴다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationResult {

    private final UUID id;
    private final NotificationChannel channel;
    private final String recipient;
    private final String title;
    private final String message;

    /** 발송 결과 — 처리 상태(6단계). 클라이언트용 축약은 웹 어댑터가 담당한다. */
    private final NotificationStatus status;

    /** 접수 시각. */
    private final LocalDateTime createdAt;

    /** 발송 완료 시각. SENT 가 아니면 null. */
    private final LocalDateTime sentAt;

    /** 발송 시도 횟수. SENT 면 "몇 번째 시도에 성공했는지"를 뜻한다. */
    private final int attemptCount;

    /** 마지막 시도 시각. */
    private final LocalDateTime lastAttemptAt;

    /** 마지막 실패 분류 코드 (HTTP_500 · TIMEOUT · CIRCUIT_OPEN · RETRY_EXHAUSTED 등). */
    private final String lastErrorCode;

    /** 마지막 실패 상세. */
    private final String lastErrorMessage;

    public static NotificationResult from(Notification notification) {
        return new NotificationResult(
                notification.getId(),
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
                notification.getLastErrorMessage());
    }
}
