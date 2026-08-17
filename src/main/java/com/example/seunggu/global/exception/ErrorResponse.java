package com.example.seunggu.global.exception;

import lombok.Getter;

@Getter
public class ErrorResponse {

    private final String code;

    private final String message;

    private final String idempotencyKey;

    private final boolean retryable;

    private ErrorResponse(ErrorCode errorCode, String message, String idempotencyKey) {
        this.code = errorCode.name();
        this.message = message;
        this.idempotencyKey = idempotencyKey;
        this.retryable = errorCode.isRetryable();
    }

    public static ErrorResponse of(ErrorCode errorCode, String idempotencyKey) {
        return new ErrorResponse(errorCode, errorCode.getDefaultMessage(), idempotencyKey);
    }

    /** 에러처리 상태 메시지가 필요할 때 사용 */
    public static ErrorResponse withDetail(ErrorCode errorCode, String idempotencyKey, String detail) {
        return new ErrorResponse(errorCode,
                detail != null ? detail : errorCode.getDefaultMessage(),
                idempotencyKey);
    }
}
