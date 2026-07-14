package com.example.seunggu.global.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.sender.EmailSender;
import com.example.seunggu.notification.sender.KakaoSender;
import com.example.seunggu.notification.sender.SmsSender;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationSenderResolverTest {

    private final KakaoSender kakao = new KakaoSender();
    private final EmailSender email = new EmailSender();
    private final SmsSender sms = new SmsSender();
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
