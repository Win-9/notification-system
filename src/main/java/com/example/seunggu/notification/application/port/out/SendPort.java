package com.example.seunggu.notification.application.port.out;

import com.example.seunggu.notification.domain.Notification;

/**
 * 아웃바운드 포트: 채널별 실제 발송.
 * 구현은 adapter.out.sender (채널별 Sender + Resolver) 가 담당한다.
 */
public interface SendPort {

    void send(Notification notification);
}
