package com.example.seunggu.notification.adapter.out.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxJpaRepository extends JpaRepository<OutboxMessageJpaEntity, Long> {

    /**
     * 미발행 메시지를 저장 순서(id)대로 잠그고 가져온다.
     */
    @Query(value = """
            SELECT * FROM outbox_message
            WHERE published = false
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessageJpaEntity> lockPendingBatch(@Param("batchSize") int batchSize);
}
