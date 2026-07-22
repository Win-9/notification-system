package com.example.seunggu.notification.adapter.out.sender;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChannelSenderAdapterTest {

    @Mock
    private NotificationSenderResolver resolver;
    @Mock
    private NotificationSender sender;
    @InjectMocks
    private ChannelSenderAdapter adapter;

    @Test
    @DisplayName("알림 채널에 맞는 발송기를 골라 발송을 위임한다")
    void send_delegatesToResolvedSender() {
        Notification n = Notification.create("key-1", NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
        when(resolver.resolve(NotificationChannel.KAKAO)).thenReturn(sender);

        adapter.send(n);

        verify(sender).send(n);
    }
}
