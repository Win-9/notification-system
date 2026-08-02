package com.example.seunggu.notification.adapter.out.sender;

import com.example.seunggu.notification.application.port.out.SendPort;
import com.example.seunggu.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 아웃바운드 어댑터(발송). SendPort 를 구현하고,
 * 채널에 맞는 발송기(전략)를 Resolver 로 골라 위임한다.
 */
@Component
@RequiredArgsConstructor
public class ChannelSenderAdapter implements SendPort {

    private final NotificationSenderResolver resolver;

    @Override
    public void send(Notification notification) {
        resolver.resolve(notification.getChannel()).send(notification);
    }
}
