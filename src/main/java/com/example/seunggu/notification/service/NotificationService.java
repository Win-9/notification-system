package com.example.seunggu.notification.service;

import com.example.seunggu.notification.domain.Notification;
import com.example.seunggu.notification.dto.NotificationRequest;
import com.example.seunggu.notification.dto.NotificationResponse;
import com.example.seunggu.notification.event.NotificationRegisteredEvent;
import com.example.seunggu.notification.repository.NotificationRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 등록/조회 서비스.
 * 등록 시 알림을 저장(PENDING)하고, 커밋 이후 비동기 발송을 위해 이벤트를 발행한다.
 * 실제 발송은 {@link NotificationDispatcher} 가 thread pool 에서 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public NotificationResponse register(NotificationRequest request) {
        validate(request);

        Notification saved = repository.save(new Notification(
                request.getChannel(),
                request.getRecipient(),
                request.getTitle(),
                request.getMessage()
        ));

        // 커밋 이후 발송이 트리거되도록 이벤트 발행 (AFTER_COMMIT 리스너).
        eventPublisher.publishEvent(new NotificationRegisteredEvent(saved.getId()));
        return NotificationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Optional<NotificationResponse> find(Long id) {
        return repository.findById(id).map(NotificationResponse::from);
    }

    private void validate(NotificationRequest request) {
        if (request.getChannel() == null) {
            throw new IllegalArgumentException("channel 은 필수입니다. (KAKAO/EMAIL/SMS)");
        }
        if (request.getRecipient() == null || request.getRecipient().isBlank()) {
            throw new IllegalArgumentException("recipient 는 필수입니다.");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("message 는 필수입니다.");
        }
    }
}
