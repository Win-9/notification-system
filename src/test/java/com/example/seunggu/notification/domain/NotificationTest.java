package com.example.seunggu.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private Notification newNotification() {
        return Notification.create("key-1", NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
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
    @DisplayName("PROCESSING 을 거쳐 markSent 하면 SENT 로 바뀌고 sentAt 이 채워진다")
    void markSent_afterProcessing() {
        Notification n = newNotification();

        n.markProcessing();
        n.markSent();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("PENDING 에서 바로 markSent 하면 상태 전이 규칙 위반으로 거부한다")
    void markSent_fromPending_rejected() {
        Notification n = newNotification();

        assertThatThrownBy(n::markSent)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("markProcessing 은 PENDING → PROCESSING 으로 전이한다")
    void markProcessing() {
        Notification n = newNotification();

        n.markProcessing();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
    }

    @Test
    @DisplayName("markRetryWait 은 PROCESSING → RETRY_WAIT 로 전이한다")
    void markRetryWait() {
        Notification n = newNotification();
        n.markProcessing();

        n.markRetryWait();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.RETRY_WAIT);
    }

    @Test
    @DisplayName("markFailed 하면 FAILED 로 바뀐다 (PENDING 에서도 허용)")
    void markFailed() {
        Notification n = newNotification();

        n.markFailed();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    @DisplayName("markDead 는 격리 종결 상태(DEAD)로 전이한다")
    void markDead() {
        Notification n = newNotification();

        n.markDead();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.DEAD);
    }
}
