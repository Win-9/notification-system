package com.example.seunggu.notification.adapter.out.sender;

import com.example.seunggu.global.exception.NotificationSendException;
import com.example.seunggu.notification.adapter.out.sender.dto.MockSendRequest;
import com.example.seunggu.notification.domain.Notification;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockNotificationApiClient {

    private final RestClient restClient;

    @CircuitBreaker(name = "mockSendApi", fallbackMethod = "sendFallback")
    public void send(Notification notification) {
        restClient.post()
                .uri("/mock/send")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MockSendRequest(
                        notification.getId().toString(),
                        notification.getChannel().name(),
                        notification.getRecipient(),
                        notification.getMessage()))
                .retrieve()
                .toBodilessEntity();
    }

    private void sendFallback(Notification notification, Throwable t) {
        log.warn("Mock Send API 호출 실패/차단 — id={}, cause={}", notification.getId(), t.toString());
        throw new NotificationSendException("Mock Send API 호출 실패 id=" + notification.getId(), t);
    }
}
