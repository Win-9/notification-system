package com.example.seunggu.global.exception;

/**
 * 멱등성 위반 예외.
 * 이미 처리 중이거나 처리된 요청이 같은 Idempotency-Key 로 중복 요청됐을 때 던진다.
 * {@link GlobalExceptionHandler} 에서 409 Conflict 로 응답한다.
 */
public class DuplicateRequestException extends RuntimeException {

    public DuplicateRequestException(String message) {
        super(message);
    }
}
