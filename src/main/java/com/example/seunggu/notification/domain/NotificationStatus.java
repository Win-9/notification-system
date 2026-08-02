package com.example.seunggu.notification.domain;

/**
 * 알림 처리 상태.
 */
public enum NotificationStatus {

    /** 접수됨. 등록 트랜잭션 커밋 시점의 초기 상태. */
    PENDING,

    /** Consumer 가 집어서 발송 처리 중. 발송 직전에 별도 트랜잭션으로 기록된다. */
    PROCESSING,

    /** 발송 성공 (종결). */
    SENT,

    /** 일시 실패 — 재시도 토픽(backoff) 대기 중. 다시 처리될 예정. */
    RETRY_WAIT,

    /** 재시도 소진 — 발송 실패 확정 기록. 운영자 개입으로 복구 가능성 있음. */
    FAILED,

    /** 자동 재시도 중단·격리 (종결). 운영자 확인 필요. */
    DEAD
}
