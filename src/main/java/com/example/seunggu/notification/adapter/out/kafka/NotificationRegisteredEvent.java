package com.example.seunggu.notification.adapter.out.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 알림이 접수(저장)되었을 때 발행되는 스프링 애플리케이션 이벤트.
 * 트랜잭션 커밋 이후(AFTER_COMMIT) Kafka 발행을 트리거하는 데 사용한다.
 */
@Getter
@AllArgsConstructor
public class NotificationRegisteredEvent {

    private final Long notificationId;
}
