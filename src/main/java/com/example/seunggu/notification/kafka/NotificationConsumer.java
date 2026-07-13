package com.example.seunggu.notification.kafka;

import com.example.seunggu.global.config.KafkaTopicConfig;
import com.example.seunggu.global.resolver.NotificationSenderResolver;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationStatus;
import com.example.seunggu.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository repository;
    private final NotificationSenderResolver senderResolver;

    @KafkaListener(
            topics = KafkaTopicConfig.NOTIFICATION_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(String notificationId) {
        Long id = Long.valueOf(notificationId);
        Notification notification = repository.findById(id).orElse(null);
        if (notification == null) {
            log.warn("발송 대상 알림 없음 id={}", id);
            return;
        }

        if (notification.getStatus() == NotificationStatus.SENT) {
            log.info("이미 발송된 알림 — 건너뜀 id={}", id);
            return;
        }

        try {
            senderResolver.resolve(notification.getChannel()).send(notification);
            notification.markSent();
            log.info("알림 발송 완료 id={}, channel={}", id, notification.getChannel());
        } catch (Exception e) {
            notification.markFailed();
            log.error("알림 발송 실패 id={}", id, e);
        }
    }
}
