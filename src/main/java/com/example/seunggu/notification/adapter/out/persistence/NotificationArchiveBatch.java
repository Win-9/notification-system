package com.example.seunggu.notification.adapter.out.persistence;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 알림 아카이빙 배치. 보존 기간이 지난 <b>종결 상태</b> 알림을
 * notification(핫) → notification_archive(콜드) 로 이관한다.
 *
 * <p>삭제가 아닌 이관이므로 데이터는 영구 보존되며, 조회가 집중되는 핫 테이블만 작게 유지된다.
 * (월별 파티셔닝은 MySQL 제약상 유니크 인덱스에 파티션 키가 포함되어야 해
 *  멱등성 백스톱이 약화되므로 채택하지 않았다 — README 참고.)
 *
 * <p>안전장치:
 * <ul>
 *   <li>종결 상태(SENT/FAILED/DEAD)만 대상 — 처리 중인 알림은 이관하지 않는다.</li>
 *   <li>복사와 삭제를 한 트랜잭션으로 묶어 원자성 보장 ({@link NotificationArchiveExecutor}).</li>
 *   <li>삭제는 아카이브에 실제 존재하는 행만(EXISTS) — 복사 누락분이 지워지지 않는다.</li>
 *   <li>청크 단위 반복 — 대량 처리 시 락·언두 로그 폭증을 피한다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationArchiveBatch {

    private final NotificationJpaRepository notificationRepository;
    private final NotificationArchiveExecutor archiveExecutor;

    @Value("${notification.archive.retention-days:30}")
    private int retentionDays;

    @Value("${notification.archive.chunk-size:1000}")
    private int chunkSize;

    @Scheduled(cron = "${notification.archive.cron:0 0 4 * * *}", scheduler = "archiveScheduler")
    public void archiveOldNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        long candidates = notificationRepository.countByCreatedAtBefore(threshold);
        if (candidates == 0) {
            log.info("아카이빙 대상 없음 (기준일 {} 이전)", threshold);
            return;
        }

        log.info("아카이빙 시작 — 기준일 {} 이전, 후보 {}건", threshold, candidates);
        int totalArchived = 0;
        int moved;
        do {
            moved = archiveExecutor.archiveChunk(threshold, chunkSize);
            totalArchived += moved;
        } while (moved > 0);

        log.info("아카이빙 완료 — {}건 이관", totalArchived);
    }
}
