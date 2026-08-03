package com.example.seunggu.notification.adapter.in.web.dto;

import java.util.List;
import java.util.UUID;

import com.example.seunggu.notification.application.port.in.CursorPage;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 커서 페이징 내역 응답. nextCursor 를 다음 요청의 cursor 파라미터로 그대로 사용한다.
 */
@Getter
@AllArgsConstructor
public class NotificationHistoryResponse {

    private final List<NotificationResponse> items;
    private final UUID nextCursor;
    private final boolean hasNext;

    public static NotificationHistoryResponse from(CursorPage page) {
        return new NotificationHistoryResponse(
                page.getItems().stream().map(NotificationResponse::from).toList(),
                page.getNextCursor(),
                page.isHasNext());
    }
}
