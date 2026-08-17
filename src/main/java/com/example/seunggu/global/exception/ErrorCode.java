package com.example.seunggu.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, false,
            "요청이 올바르지 않습니다."),

    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, false,
            "요청 본문을 해석할 수 없습니다. JSON 형식을 확인해주세요."),

    MISSING_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, false,
            "Idempotency-Key 헤더는 필수입니다."),

    DUPLICATE_IN_PROGRESS(HttpStatus.CONFLICT, true,
            "이미 처리 중인 요청입니다.", 1),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, true,
            "일시적인 오류가 발생했습니다. 같은 Idempotency-Key 로 재시도해주세요.");

    private final HttpStatus status;
    private final boolean retryable;
    private final String defaultMessage;
    private final Integer retryAfterSeconds;

    ErrorCode(HttpStatus status, boolean retryable, String defaultMessage) {
        this(status, retryable, defaultMessage, null);
    }

    ErrorCode(HttpStatus status, boolean retryable, String defaultMessage, Integer retryAfterSeconds) {
        this.status = status;
        this.retryable = retryable;
        this.defaultMessage = defaultMessage;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
