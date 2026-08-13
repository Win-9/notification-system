package com.example.seunggu.notification.adapter.out.persistence;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아카이빙 청크 단위 트랜잭션 실행자.
 *
 * <p>배치({@link NotificationArchiveBatch})와 <b>별도 빈으로 분리</b>한 이유:
 * 같은 클래스 내부 호출은 스프링 프록시를 거치지 않아 @Transactional 이 무시된다(셀프 인보케이션).
 * 청크마다 독립 트랜잭션이 필요하므로 경계를 다른 빈으로 빼야 한다.
 */
@Component
@RequiredArgsConstructor
public class NotificationArchiveExecutor {

    private final NotificationJpaRepository notificationRepository;
    private final NotificationArchiveJpaRepository archiveRepository;

    /**
     * 청크 하나를 이관
     * @return 이관된 건수 (0 이면 대상 소진)
     */
    @Transactional
    public int archiveChunk(LocalDateTime threshold, int chunkSize) {
        int copied = archiveRepository.copyChunkFromHot(threshold, chunkSize);
        if (copied == 0) {
            return 0;
        }
        int deleted = notificationRepository.deleteArchivedChunk(threshold, chunkSize);
        if (copied != deleted) {
            // 정상 흐름에서는 발생하지 않는다. 불일치 시 트랜잭션을 되돌려 수동 확인을 유도한다.
            throw new IllegalStateException(
                    "아카이빙 건수 불일치 — copied=%d, deleted=%d".formatted(copied, deleted));
        }
        return deleted;
    }
}
