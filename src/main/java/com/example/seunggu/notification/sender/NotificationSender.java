package com.example.seunggu.notification.sender;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;

public interface NotificationSender {
    NotificationChannel channel();
    void send(Notification notification);
}
