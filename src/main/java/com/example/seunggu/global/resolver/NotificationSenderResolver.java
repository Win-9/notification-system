package com.example.seunggu.global.resolver;

import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.sender.NotificationSender;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NotificationSenderResolver {
    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationSenderResolver(List<NotificationSender> senders) {
        this.senders = senders.stream()
                .collect(Collectors.toMap(NotificationSender::channel, s -> s));
    }

    public NotificationSender resolve(NotificationChannel channel) {
        return senders.get(channel);
    }
}
