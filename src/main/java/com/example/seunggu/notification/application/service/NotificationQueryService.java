package com.example.seunggu.notification.application.service;

import com.example.seunggu.notification.application.port.in.FindNotificationQuery;
import com.example.seunggu.notification.application.port.in.NotificationResult;
import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 상태 조회 유스케이스.
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService implements FindNotificationQuery {

    private final NotificationPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationResult> findById(Long id) {
        return persistencePort.findById(id).map(NotificationResult::from);
    }
}
