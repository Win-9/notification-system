package com.example.seunggu.notification.adapter.in.web.dto;

import com.example.seunggu.notification.domain.NotificationStatus;

/**
 * 클라이언트에 노출하는 발송 결과. 내부 6단계 상태를 3단계로 축약한다.
 * 축약 규칙은 표현의 관심사이므로 웹 어댑터가 소유한다 — 코어는 6단계를 그대로 다룬다.
 */
public enum NotificationStatusView {

    /** 진행 중 — 접수·처리중·재시도 대기. 클라이언트는 계속 폴링하면 된다. */
    PENDING,

    /** 발송 성공 (종결). */
    SUCCESS,

    /** 발송 실패 (종결). */
    FAIL;

    /**
     * 도메인 상태를 공개 표현으로 변환한다.
     * <p>{@code default} 를 두지 않는다 — 상태가 추가되면 컴파일 에러로 드러나야 하고,
     * 그렇지 않으면 새 상태가 조용히 {@code PENDING} 으로 새어나가 클라이언트가 무한 폴링한다.
     */
    public static NotificationStatusView from(NotificationStatus status) {
        return switch (status) {
            case PENDING, PROCESSING, RETRY_WAIT -> PENDING;
            case SENT                            -> SUCCESS;
            case FAILED, DEAD                    -> FAIL;
        };
    }
}
