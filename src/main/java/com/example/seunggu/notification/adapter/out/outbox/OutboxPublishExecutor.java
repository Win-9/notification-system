package com.example.seunggu.notification.adapter.out.outbox;

import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox 배치 발행 실행자 — 조회(잠금) · 발행 · 마킹을 한 트랜잭션으로 수행한다.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublishExecutor {

    private final OutboxJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${notification.outbox.ack-timeout-ms:5000}")
    private long ackTimeoutMs;

    /**
     * 미발행 배치를 잠그고 발행한다.
     * @return 발행에 성공해 마킹된 건수
     */
    @Transactional
    public int publishBatch(int batchSize) {
        List<OutboxMessageJpaEntity> pending = outboxRepository.lockPendingBatch(batchSize);
        int publishedCount = 0;

        for (OutboxMessageJpaEntity message : pending) {
            try {
                // 브로커 ack 까지 동기 대기 — 성공이 확인된 것만 마킹한다(유실 방향 실패 불가).
                kafkaTemplate.send(message.getTopic(), message.getMessageKey(), message.getPayload())
                        .get(ackTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("outbox 발행 중단(인터럽트) — 남은 배치는 다음 주기에 처리 outboxId={}", message.getId());
                break;
            } catch (Exception e) {
                // 실패한 건만 건너뛴다. 마킹하지 않으므로 다음 주기에 다시 시도된다.
                log.warn("outbox 발행 실패 — 건너뜀, 다음 주기 재시도 outboxId={}", message.getId(), e);
                continue;
            }

            message.markPublished();
            publishedCount++;
        }
        return publishedCount;
    }
}
