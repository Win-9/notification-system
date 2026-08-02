package com.example.seunggu.notification.adapter.out.outbox;

import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox 릴레이. 미발행 메시지를 주기적으로 폴링해 Kafka 로 발행한다.
 * 브로커 ack 를 확인한 뒤에만 published 로 마킹하므로 유실이 없다.
 * 발행 후 마킹 전에 죽으면 다음 주기에 재발행될 수 있다(at-least-once) —
 * 중복은 Consumer 의 멱등 가드(status == SENT 스킵)가 흡수한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void relay() {
        List<OutboxMessageJpaEntity> pending = outboxRepository.findTop100ByPublishedFalseOrderByIdAsc();
        for (OutboxMessageJpaEntity message : pending) {
            try {
                // 브로커 ack 까지 동기 대기 — 성공이 확인된 것만 마킹한다.
                kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                        .get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // 발행 실패 — 마킹하지 않고 중단, 다음 주기에 이 메시지부터 재시도 (저장 순서 유지).
                log.warn("outbox 발행 실패 — 다음 주기에 재시도 outboxId={}", message.getId(), e);
                return;
            }

            message.markPublished();
            outboxRepository.save(message);
            log.info("outbox 발행 완료 outboxId={}, key={}", message.getId(), message.getMessageKey());
        }
    }
}
