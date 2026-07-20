package com.example.seunggu.notification.application.port.out;

import java.util.UUID;

/**
 * 아웃바운드 포트: 알림 등록 이벤트 발행.
 * 트랜잭션 안에서 호출되며, 구현(adapter.out.outbox)은 발행 예약을
 * 같은 트랜잭션의 outbox 테이블에 기록해 유실 없는 발행을 보장한다.
 */
public interface RegisteredEventPort {

    void publishRegistered(UUID notificationId);
}
