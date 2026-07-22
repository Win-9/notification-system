package com.example.seunggu.notification.adapter.out.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.seunggu.notification.domain.NotificationChannel;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationSenderResolverTest {

    private final MockNotificationApiClient apiClient = mock(MockNotificationApiClient.class);
    private final KakaoSender kakao = new KakaoSender(apiClient);
    private final EmailSender email = new EmailSender(apiClient);
    private final SmsSender sms = new SmsSender(apiClient);
    private final NotificationSenderResolver resolver =
            new NotificationSenderResolver(List.of(kakao, email, sms));

    @Test
    @DisplayName("채널에 맞는 발송기를 반환한다")
    void resolveByChannel() {
        assertThat(resolver.resolve(NotificationChannel.KAKAO)).isSameAs(kakao);
        assertThat(resolver.resolve(NotificationChannel.EMAIL)).isSameAs(email);
        assertThat(resolver.resolve(NotificationChannel.SMS)).isSameAs(sms);
    }
}
