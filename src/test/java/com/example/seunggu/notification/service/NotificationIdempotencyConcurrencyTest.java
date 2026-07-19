package com.example.seunggu.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.dto.NotificationRequest;
import com.example.seunggu.notification.repository.NotificationRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;


@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrenttest;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Testcontainers
class NotificationIdempotencyConcurrencyTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository repository;

    private NotificationRequest request() {
        return new NotificationRequest(NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
    }

    @Test
    @DisplayName("같은 키로 동시에 10개 요청을 보내면 1건만 성공하고 나머지는 중복 처리된다")
    void concurrentSameKey_onlyOneSucceeds() throws InterruptedException {
        int threads = 10;
        String key = "concurrent-key-1";

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    notificationService.register(key, request());
                    success.incrementAndGet();
                } catch (DuplicateRequestException e) {
                    duplicate.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        boolean finished = pool.awaitTermination(20, TimeUnit.SECONDS);

        assertThat(finished).isTrue();
        assertThat(success.get()).isEqualTo(1);
        assertThat(duplicate.get()).isEqualTo(threads - 1);
        assertThat(repository.count()).isEqualTo(1L);
    }
}
