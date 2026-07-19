package com.example.seunggu.notification.adapter.out.sender;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsSender implements NotificationSender {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(Notification notification) {
        log.info("[SMS] to={}, msg={}", notification.getRecipient(), notification.getMessage());
    }
}
