package com.example.seunggu.notification.adapter.out.sender;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailSender implements NotificationSender {
    private final MockNotificationApiClient mockApiClient;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        mockApiClient.send(notification);
        log.info("[이메일] to={}, msg={}", notification.getRecipient(), notification.getMessage());
    }
}
