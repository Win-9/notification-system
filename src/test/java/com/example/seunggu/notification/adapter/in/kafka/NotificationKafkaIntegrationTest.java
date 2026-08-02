package com.example.seunggu.notification.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.application.port.out.SendPort;
import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import java.time.Duration;
import java.util.UUID;
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
        topics = {NotificationKafkaIntegrationTest.TOPIC},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class NotificationKafkaIntegrationTest {

    /** application.properties 의 notification.topic 과 동일해야 한다 (@EmbeddedKafka 는 상수만 허용). */
    static final String TOPIC = "notification-events";

    // 멱등성(Redis)은 발송 경로와 무관하므로 목킹해 실제 Redis 의존을 없앤다.
    @MockitoBean
    private RedissonClient redissonClient;

    // 실제 채널 발송(외부 HTTP)은 목킹 — 발송 성공 시 상태 전이만 검증한다.
    @MockitoBean
    private SendPort sendPort;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NotificationPersistencePort persistencePort;

    @Test
    @DisplayName("토픽에 알림 ID 를 발행하면 Consumer 가 소비해 상태를 SENT 로 바꾼다")
    void publishedMessage_isConsumedAndMarkedSent() {
        Notification saved = persistencePort.save(Notification.create(
                "key-int-1", NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용"));
        UUID id = saved.getId();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);

        kafkaTemplate.send(TOPIC, id.toString(), id.toString());

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    Notification found = persistencePort.findById(id).orElseThrow();
                    assertThat(found.getStatus()).isEqualTo(NotificationStatus.SENT);
                    assertThat(found.getSentAt()).isNotNull();
                });
    }
}
