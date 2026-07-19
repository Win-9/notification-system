package com.example.seunggu.notification.application.port.out;

/**
 * 아웃바운드 포트: 알림 등록 이벤트 발행.
 * 트랜잭션 안에서 호출되며, 구현(adapter.out.kafka)은 커밋 이후(AFTER_COMMIT)에만
 * 실제 Kafka 발행이 일어나도록 보장한다.
 */
public interface RegisteredEventPort {

    void publishRegistered(Long notificationId);
}
