package com.example.seunggu.notification.dto;

import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 멱등성 상태 레코드. Redis 에 JSON 으로 저장되어 <b>멱등성의 진실(source of truth)</b> 역할을 한다.
 *
 * <p>- {@code PENDING} : 최초 요청이 선점하고 처리 중.
 * <p>- {@code DONE}    : 처리 완료. 같은 키로 재요청이 오면 저장된 {@link #toResponse()} 를 그대로 돌려준다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Jackson 역직렬화용
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class IdempotencyDto {

    public static final String PENDING = "PENDING";
    public static final String DONE = "DONE";

    private String state;
    private Long id;
    private NotificationChannel channel;
    private String recipient;
    private NotificationStatus status;

    public static IdempotencyDto pending() {
        return new IdempotencyDto(PENDING, null, null, null, null);
    }

    public static IdempotencyDto done(NotificationResponse response) {
        return new IdempotencyDto(
                DONE,
                response.getId(),
                response.getChannel(),
                response.getRecipient(),
                response.getStatus());
    }

    @JsonIgnore
    public boolean isPending() {
        return PENDING.equals(state);
    }

    public NotificationResponse toResponse() {
        return new NotificationResponse(id, channel, recipient, status);
    }
}
