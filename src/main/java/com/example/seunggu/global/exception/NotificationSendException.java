package com.example.seunggu.global.exception;

/**
 * 알림 발송 실패 예외.
 * Consumer 가 발송 실패를 감지해 RETRY_WAIT 기록 후 재시도를 유도할 때 던진다.
 * 이 예외가 리스너 밖으로 전파되어야 @RetryableTopic 재시도(WAIT 토픽)가 동작한다.
 */
public class NotificationSendException extends RuntimeException {

    public NotificationSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
