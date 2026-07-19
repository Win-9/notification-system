package com.example.seunggu.notification.adapter.out.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxJpaRepository extends JpaRepository<OutboxMessageJpaEntity, Long> {

    /** 미발행 메시지를 저장 순서(id)대로 조회한다. */
    List<OutboxMessageJpaEntity> findTop100ByPublishedFalseOrderByIdAsc();
}
