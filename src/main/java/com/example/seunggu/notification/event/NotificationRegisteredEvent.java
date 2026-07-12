package com.example.seunggu.notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 알림이 접수(저장)되었을 때 발행되는 이벤트.
 * 트랜잭션 커밋 이후(AFTER_COMMIT) 비동기 발송을 트리거하는 데 사용한다.
 * (추후 Kafka 도입 시 이 지점을 Kafka Producer 발행으로 대체할 수 있다.)
 */
@Getter
@AllArgsConstructor
public class NotificationRegisteredEvent {

    private final Long notificationId;
}
