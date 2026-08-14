package com.example.seunggu.notification.adapter.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.seunggu.notification.application.port.in.NotificationResult;
import com.example.seunggu.notification.domain.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 알림 등록/조회/내역 응답
 */
@Getter
@AllArgsConstructor
public class NotificationResponse {

    private final UUID id;
    private final NotificationChannel channel;
    private final String recipient;
    private final String title;
    private final String message;

    /** 발송 결과 — PENDING(진행 중) / SUCCESS / FAIL. 내부 6단계를 축약한 값이다. */
    private final NotificationStatusView status;

    /** 접수 시각. */
    private final LocalDateTime createdAt;

    /** 발송 완료 시각. SUCCESS 가 아니면 null. */
    private final LocalDateTime sentAt;

    /** 발송 시도 횟수. */
    private final int attemptCount;

    /** 마지막 시도 시각. */
    private final LocalDateTime lastAttemptAt;

    /** 마지막 실패 분류 코드. */
    private final String lastErrorCode;

    /** 마지막 실패 상세. */
    private final String lastErrorMessage;

    public static NotificationResponse from(NotificationResult result) {
        return new NotificationResponse(
                result.getId(),
                result.getChannel(),
                result.getRecipient(),
                result.getTitle(),
                result.getMessage(),
                NotificationStatusView.from(result.getStatus()),
                result.getCreatedAt(),
                result.getSentAt(),
                result.getAttemptCount(),
                result.getLastAttemptAt(),
                result.getLastErrorCode(),
                result.getLastErrorMessage()
        );
    }
}
