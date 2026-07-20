package com.example.seunggu.notification.adapter.out.sender;

import com.example.seunggu.notification.adapter.out.sender.dto.MockSendRequest;
import com.example.seunggu.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class MockNotificationApiClient {

    private final RestClient restClient;

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
}
