package com.example.seunggu.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.adapter.out.persistence.NotificationJpaRepository;
import com.example.seunggu.notification.application.port.in.RegisterNotificationCommand;
import com.example.seunggu.notification.application.port.in.RegisterNotificationUseCase;
import com.example.seunggu.notification.domain.NotificationChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

    /**
     * Outbox 릴레이와 @RetryableTopic 가 쓰는 Kafka 발행은 이 테스트의 관심사가 아니다.
     * @Primary 목 빈을 추가로 등록해, 타입 주입(OutboxRelay)과 KafkaOperations 단일 조회
     * (@RetryableTopic) 모두 이 목을 선택하게 한다 — 자동설정 빈은 남지만 사용되지 않는다.
     */
    @TestConfiguration
    static class MockKafkaConfig {
        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> mockKafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }

    @Autowired
    private RegisterNotificationUseCase registerUseCase;

    @Autowired
    private NotificationJpaRepository jpaRepository;

    private RegisterNotificationCommand command() {
        return new RegisterNotificationCommand(NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
    }

    @Test
    @DisplayName("같은 키로 동시에 10개 요청을 보내도 실제 저장은 정확히 1건만 일어난다")
    void concurrentSameKey_persistsExactlyOnce() throws InterruptedException {
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
                    // 선점 승자는 저장, 나머지는 기존 결과 재생(정상) 또는 처리 중이면 409.
                    registerUseCase.register(key, command());
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
        // 핵심 불변식: 동시 요청이 몇이든 물리적으로 저장된 알림은 단 1건.
        assertThat(jpaRepository.count()).isEqualTo(1L);
        // 모든 스레드는 성공(저장/재생) 또는 409 중 하나로 정의된 결과를 낸다.
        assertThat(success.get() + duplicate.get()).isEqualTo(threads);
        assertThat(success.get()).isGreaterThanOrEqualTo(1);
    }
}
