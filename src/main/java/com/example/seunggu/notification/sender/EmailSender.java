package com.example.seunggu.notification.sender;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailSender implements NotificationSender{
    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        log.info("[이메일] to={}, msg={}", notification.getRecipient(), notification.getMessage());
    }
}
