package com.example.seunggu.notification.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchivePartitionMaintainer {
    private final JdbcTemplate jdbcTemplate;


    @Value("${notification.archive.partition.months-ahead:2}")
    private int monthsAhead;

    /** 파티션 이름 규칙 — p + yyyy + MM (예: p202608). */
    private static final Pattern PARTITION_NAME = Pattern.compile("^p(\\d{4})(\\d{2})$");
    private static final String TABLE = "notification_archive";

    @Scheduled(cron = "${notification.archive.partition.cron:0 30 3 * * *}",
            scheduler = "archiveScheduler")
    public void createArchivePartition() {
        Set<String> existingPartitionSet = getExistingSet();
        Optional<YearMonth> latest = existingPartitionSet.stream()
                .map(PARTITION_NAME::matcher)
                .filter(Matcher::matches)
                .map(m -> YearMonth.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))))
                .max(Comparator.naturalOrder());

        if (latest.isEmpty()) {
            log.error("{} 가 파티션 테이블이 아니다 — 초기 DDL 확인 필요", TABLE);
            return;
        }

        YearMonth base = YearMonth.now();
        YearMonth last = latest.get();

        for (int i = 0; i <= monthsAhead; i++) {
            YearMonth target = base.plusMonths(i);
            if (!target.isAfter(last)) {
                continue;
            }

            String name = "p%d%02d".formatted(target.getYear(), target.getMonthValue());
            LocalDate lessThan = target.plusMonths(1).atDay(1);
            jdbcTemplate.execute(
                    "ALTER TABLE %s ADD PARTITION (PARTITION %s VALUES LESS THAN ('%s'))"
                            .formatted(TABLE, name, lessThan));
            log.info("아카이브 파티션 추가 — {} (< {})", name, lessThan);
            last = target;
        }
    }

    private Set<String> getExistingSet() {
        return new HashSet<>(jdbcTemplate.queryForList("""
                SELECT partition_name
                FROM information_schema.partitions
                WHERE table_schema = DATABASE()
                  AND table_name = 'notification_archive'
                  AND partition_name IS NOT NULL
                """, String.class));
    }
}
