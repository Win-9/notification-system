package com.example.seunggu.notification.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.seunggu.global.config.KafkaTopicConfig;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import com.example.seunggu.notification.repository.NotificationRepository;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notitest;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopicConfig.NOTIFICATION_TOPIC},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class NotificationKafkaIntegrationTest {

    @MockitoBean
    private RedissonClient redissonClient;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NotificationRepository repository;

    @Test
    @DisplayName("토픽에 알림 ID 를 발행하면 Consumer 가 소비해 상태를 SENT 로 바꾼다")
    void publishedMessage_isConsumedAndMarkedSent() {
        Notification saved = repository.save(new Notification(
                "key-int-1", NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용"));
        Long id = saved.getId();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);

        kafkaTemplate.send(KafkaTopicConfig.NOTIFICATION_TOPIC, id.toString(), id.toString());

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Notification found = repository.findById(id).orElseThrow();
                    assertThat(found.getStatus()).isEqualTo(NotificationStatus.SENT);
                    assertThat(found.getSentAt()).isNotNull();
                });
    }
}
