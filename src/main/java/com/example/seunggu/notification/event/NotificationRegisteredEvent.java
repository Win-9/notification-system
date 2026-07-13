package com.example.seunggu.notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationRegisteredEvent {

    private final Long notificationId;
}
