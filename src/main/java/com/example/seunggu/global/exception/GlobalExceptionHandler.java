package com.example.seunggu.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    /** 검증 실패 — 위반 종류가 여럿이라 예외 메시지를 노출한다(우리가 작성한 문구라 안전). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidation(IllegalArgumentException e,
                                                          HttpServletRequest request) {
        return build(ErrorCode.VALIDATION_FAILED, e.getMessage(), request);
    }

    /** 같은 멱등키가 처리 중 — 아직 돌려줄 결과가 없다. */
    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateRequestException e,
                                                         HttpServletRequest request) {
        return build(ErrorCode.DUPLICATE_IN_PROGRESS, null, request);
    }

    /** 헤더 누락 — catch-all 이 500 으로 삼키지 않도록 명시적으로 잡는다. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e,
                                                             HttpServletRequest request) {
        return build(ErrorCode.MISSING_IDEMPOTENCY_KEY, null, request);
    }

    /** 본문 파싱 실패 — Jackson 내부 메시지는 노출하지 않는다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformed(HttpMessageNotReadableException e,
                                                         HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_REQUEST, null, request);
    }

    /** 예상 밖 오류 — 상세는 로그에만, 응답엔 고정 문구. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("처리되지 않은 예외 key={}", request.getHeader(IDEMPOTENCY_KEY_HEADER), e);
        return build(ErrorCode.INTERNAL_ERROR, null, request);
    }

    private static ResponseEntity<ErrorResponse> build(ErrorCode errorCode, String detail, HttpServletRequest request) {
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(errorCode.getStatus());
        if (errorCode.getRetryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(errorCode.getRetryAfterSeconds()));
        }

        return builder.body(
                detail != null
                        ? ErrorResponse.withDetail(errorCode, idempotencyKey, detail)
                        : ErrorResponse.of(errorCode, idempotencyKey)
        );
    }
}
