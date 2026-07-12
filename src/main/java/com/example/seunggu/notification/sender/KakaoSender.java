package com.example.seunggu.notification.sender;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KakaoSender implements NotificationSender{
    @Override
    public NotificationChannel channel() {
        return NotificationChannel.KAKAO;
    }

    @Override
    public void send(Notification notification) {
        log.info("[카카오톡] to={}, msg={}", notification.getRecipient(), notification.getMessage());
    }
}
