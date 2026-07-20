package com.example.seunggu.notification.application.port.in;

import java.util.UUID;

/**
 * 인바운드 포트: 알림 발송.
 * 메시지 큐 어댑터(Kafka Consumer)가 호출한다.
 */
public interface SendNotificationUseCase {

    /** 알림을 발송하고 SENT 로 확정한다. 실패 시 예외를 전파해 재시도를 유도한다. */
    void send(UUID notificationId);

    /** 재시도 소진 후(DLT) 최종 실패를 확정한다. */
    void markFailed(UUID notificationId);

    // 발송 실패 기록
    void markRetryWait(UUID notificationId);

    // 복구 불가
    void markDead(UUID notificationId);
}
