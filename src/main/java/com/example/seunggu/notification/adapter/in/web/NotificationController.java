package com.example.seunggu.notification.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.example.seunggu.notification.adapter.in.web.dto.NotificationRequest;
import com.example.seunggu.notification.adapter.in.web.dto.NotificationResponse;
import com.example.seunggu.notification.application.port.in.FindNotificationQuery;
import com.example.seunggu.notification.application.port.in.NotificationResult;
import com.example.seunggu.notification.application.port.in.RegisterNotificationCommand;
import com.example.seunggu.notification.application.port.in.RegisterNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인바운드 어댑터(HTTP). 웹 요청을 유스케이스 호출로 번역한다.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final RegisterNotificationUseCase registerUseCase;
    private final FindNotificationQuery findQuery;

    /** 알림 등록. 접수만 하고 실제 발송은 비동기로 진행되므로 202 Accepted 를 반환한다. */
    @PostMapping
    public ResponseEntity<NotificationResponse> register(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody NotificationRequest request) {
        NotificationResult result = registerUseCase.register(idempotencyKey,
                new RegisterNotificationCommand(
                        request.getChannel(),
                        request.getRecipient(),
                        request.getTitle(),
                        request.getMessage()
                ));
        return ResponseEntity.accepted().body(NotificationResponse.from(result));
    }

    /** 알림 상태 조회 (발송 결과 확인용). */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> get(@PathVariable UUID id) {
        return ResponseEntity.of(findQuery.findById(id).map(NotificationResponse::from));
    }

    /** 요청자별 최근 7일 내역 조회 (최신순 페이징). */
    @GetMapping("/history")
    public ResponseEntity<List<NotificationResponse>> history(
            @RequestParam String recipient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<NotificationResponse> body = findQuery.findRecentByRecipient(recipient, page, size).stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }
}
