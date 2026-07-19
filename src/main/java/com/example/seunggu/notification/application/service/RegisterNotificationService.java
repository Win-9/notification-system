package com.example.seunggu.notification.application.service;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.application.port.in.NotificationResult;
import com.example.seunggu.notification.application.port.in.RegisterNotificationCommand;
import com.example.seunggu.notification.application.port.in.RegisterNotificationUseCase;
import com.example.seunggu.notification.application.port.out.IdempotencyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 알림 등록 유스케이스.
 * 멱등성 판별(Redis 선점) → 저장 + 이벤트 발행(트랜잭션) 순으로 조율한다.
 * Redis 마커가 유실돼도 DB 유니크 제약(백스톱)이 중복을 막는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterNotificationService implements RegisterNotificationUseCase {

    private final IdempotencyPort idempotencyPort;
    private final NotificationRegistrar registrar;

    @Override
    public NotificationResult register(String idempotencyKey, RegisterNotificationCommand command) {
        validate(command);

        // SETNX + TTL — 최초 요청만 선점에 성공한다.
        if (!idempotencyPort.tryClaim(idempotencyKey)) {
            throw new DuplicateRequestException("이미 처리된(또는 처리 중인) 요청입니다: " + idempotencyKey);
        }

        try {
            return registrar.persist(idempotencyKey, command);
        } catch (DuplicateRequestException e) {
            // DB 유니크 제약(백스톱)에 걸린 경우 — 중복이 맞으므로 마커를 유지한다.
            log.info("DB 유니크 제약으로 중복 방지 (Redis 마커 유실 추정) key={}", idempotencyKey);
            throw e;
        } catch (RuntimeException e) {
            // 처리 실패 → 선점을 해제해 같은 키의 재시도를 허용한다.
            idempotencyPort.release(idempotencyKey);
            throw e;
        }
    }

    private void validate(RegisterNotificationCommand command) {
        if (command.getChannel() == null) {
            throw new IllegalArgumentException("channel 은 필수입니다. (KAKAO/EMAIL/SMS)");
        }
        if (command.getRecipient() == null || command.getRecipient().isBlank()) {
            throw new IllegalArgumentException("recipient 는 필수입니다.");
        }
        if (command.getMessage() == null || command.getMessage().isBlank()) {
            throw new IllegalArgumentException("message 는 필수입니다.");
        }
    }
}
