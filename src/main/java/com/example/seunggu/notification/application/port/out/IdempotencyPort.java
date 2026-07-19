package com.example.seunggu.notification.application.port.out;

/**
 * 아웃바운드 포트: 멱등성 키 선점.
 * 구현은 adapter.out.redis (Redisson SETNX + TTL) 가 담당한다.
 */
public interface IdempotencyPort {

    /**
     * 키를 원자적으로 선점한다.
     * @return true = 최초 요청 (선점 성공), false = 이미 처리됐거나 처리 중
     */
    boolean tryClaim(String key);

    /** 선점을 해제해 같은 키의 재시도를 허용한다 (처리 실패 시 보상). */
    void release(String key);
}
