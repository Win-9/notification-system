package com.example.seunggu.notification.application.port.in;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 커서 페이징 결과. 클라이언트는 nextCursor 를 다음 요청의 cursor 로 그대로 반납한다.
 * 서버는 읽은 위치를 기억하지 않는다(무상태) — 위치 상태는 커서에 담겨 클라이언트가 운반한다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CursorPage {

    private final List<NotificationResult> items;

    /** 다음 페이지 조회에 사용할 커서. 마지막 페이지면 null. */
    private final UUID nextCursor;

    private final boolean hasNext;

    public static CursorPage of(List<NotificationResult> items, UUID nextCursor, boolean hasNext) {
        return new CursorPage(items, nextCursor, hasNext);
    }

    public static CursorPage last(List<NotificationResult> items) {
        return new CursorPage(items, null, false);
    }
}
