package com.example.seunggu.notification.adapter.out.sender;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;

/**
 * 채널별 발송기 SPI. 새 채널 추가 시 이 인터페이스의 @Component 구현만 추가하면
 * Resolver 에 자동 편입된다 (OCP).
 */
public interface NotificationSender {

    NotificationChannel channel();

    void send(Notification notification);
}
