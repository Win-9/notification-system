package com.example.seunggu.notification.adapter.out.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox 릴레이. 미발행 메시지를 주기적으로 폴링해 Kafka 로 발행한다.
 *
 * <p>브로커 ack 를 확인한 뒤에만 published 로 마킹하므로 유실이 없다.
 * 발행 후 마킹 전에 죽으면 다음 주기에 재발행될 수 있다(at-least-once) —
 * 중복은 Consumer 의 멱등 가드(status == SENT 스킵)가 흡수한다.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxPublishExecutor publishExecutor;

    @Value("${notification.outbox.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${notification.outbox.poll-interval:1000}", scheduler = "outboxScheduler")
    public void relay() {
        int published = publishExecutor.publishBatch(batchSize);
        if (published > 0) {
            log.debug("outbox 배치 발행 완료 — {}건", published);
        }
    }
}
