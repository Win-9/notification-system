package com.example.seunggu.notification.adapter.out.sender;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.concurrent.TimeoutException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 발송 실패 원인을 운영자가 읽을 수 있는 코드로 분류한다.
 */
final class SendErrorClassifier {

    private SendErrorClassifier() {
    }

    /** 서킷 차단으로 호출 자체가 이뤄지지 않음 — 외부 API 장애가 지속되는 상태. */
    static final String CIRCUIT_OPEN = "CIRCUIT_OPEN";
    /** 응답 시간 초과 또는 연결 실패. */
    static final String TIMEOUT = "TIMEOUT";

    static String classify(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof CallNotPermittedException) {
                return CIRCUIT_OPEN;
            }
            if (cause instanceof RestClientResponseException httpError) {
                return "HTTP_" + httpError.getStatusCode().value();
            }
            if (cause instanceof ResourceAccessException || cause instanceof TimeoutException) {
                return TIMEOUT;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return t == null ? "UNKNOWN" : t.getClass().getSimpleName();
    }
}
