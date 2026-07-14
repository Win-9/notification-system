package com.example.seunggu.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private Notification newNotification() {
        return new Notification("key-1", NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
    }

    @Test
    @DisplayName("생성 시 상태는 PENDING, createdAt 세팅, sentAt 은 null")
    void createdAsPending() {
        Notification n = newNotification();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getCreatedAt()).isNotNull();
        assertThat(n.getSentAt()).isNull();
    }

    @Test
    @DisplayName("markSent 하면 SENT 로 바뀌고 sentAt 이 채워진다")
    void markSent() {
        Notification n = newNotification();

        n.markSent();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("markFailed 하면 FAILED 로 바뀐다")
    void markFailed() {
        Notification n = newNotification();

        n.markFailed();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }
}
