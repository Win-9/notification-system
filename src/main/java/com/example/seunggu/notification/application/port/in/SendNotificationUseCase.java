package com.example.seunggu.notification.application.port.in;

import java.util.UUID;

/**
 * 인바운드 포트: 알림 발송.
 * 메시지 큐 어댑터(Kafka Consumer)가 호출한다.
 *
 * <p>실패 기록 메서드는 원인 분류(errorCode)와 상세(errorMessage)를 함께 받는다 —
 * "몇 번 시도하고 왜 실패했는지"를 로그가 아니라 DB 에 남겨 상태 조회로 확인할 수 있게 한다.
 */
public interface SendNotificationUseCase {

    /** 알림을 발송하고 SENT 로 확정한다. 실패 시 예외를 전파해 재시도를 유도한다. */
    void send(UUID notificationId);

    /** 발송 일시 실패 기록 — RETRY_WAIT (재시도 대기). */
    void markRetryWait(UUID notificationId, String errorCode, String errorMessage);

    /** 재시도 소진 후(DLT) 최종 실패를 확정한다 — FAILED (운영자 복구 여지). */
    void markFailed(UUID notificationId, String errorCode, String errorMessage);

    /** 복구 불가 판정 — DEAD (격리, 운영자 확인 필요). */
    void markDead(UUID notificationId, String errorCode, String errorMessage);
}
