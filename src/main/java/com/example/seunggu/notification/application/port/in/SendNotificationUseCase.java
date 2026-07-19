package com.example.seunggu.notification.application.port.in;

/**
 * 인바운드 포트: 알림 발송.
 * 메시지 큐 어댑터(Kafka Consumer)가 호출한다.
 */
public interface SendNotificationUseCase {

    /** 알림을 발송하고 SENT 로 확정한다. 실패 시 예외를 전파해 재시도를 유도한다. */
    void send(Long notificationId);

    /** 재시도 소진 후(DLT) 최종 실패를 확정한다. */
    void markFailed(Long notificationId);
}
