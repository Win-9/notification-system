package com.example.seunggu.notification.adapter.out.sender;

import com.example.seunggu.notification.domain.NotificationChannel;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 채널 → 발송기 매핑. 스프링이 주입한 NotificationSender 구현 목록으로 맵을 구성한다.
 */
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
