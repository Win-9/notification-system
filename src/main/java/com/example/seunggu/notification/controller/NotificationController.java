package com.example.seunggu.notification.controller;

import com.example.seunggu.notification.dto.NotificationRequest;
import com.example.seunggu.notification.dto.NotificationResponse;
import com.example.seunggu.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 알림 등록. 접수만 하고 실제 발송은 비동기로 진행되므로 202 Accepted 를 반환한다. */
    @PostMapping
    public ResponseEntity<NotificationResponse> register(@RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.register(request);
        return ResponseEntity.accepted().body(response);
    }

    /** 알림 상태 조회 (발송 결과 확인용). */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> get(@PathVariable Long id) {
        return ResponseEntity.of(notificationService.find(id));
    }
}
