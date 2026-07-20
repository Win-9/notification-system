package com.example.seunggu.notification.application.service;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.application.port.in.NotificationResult;
import com.example.seunggu.notification.application.port.in.RegisterNotificationCommand;
import com.example.seunggu.notification.application.port.in.RegisterNotificationUseCase;
import com.example.seunggu.notification.application.port.out.IdempotencyPort;
import com.example.seunggu.notification.application.port.out.NotificationPersistencePort;
import com.example.seunggu.notification.domain.Notification;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 알림 등록 유스케이스.
 * 멱등성 판별(Redis 선점) → 저장 + 이벤트 발행(트랜잭션) 순으로 조율한다.
 *
 * <p>중복 판정의 원칙: 예외 타입이나 마커 존재만으로 추측하지 않고,
 * <b>"이 키로 실제 저장된 알림이 있는가"(findByIdempotencyKey)</b> 를 진실로 삼는다.
 * <ul>
 *   <li>처리 완료된 키의 재요청 → 첫 요청과 동일한 응답을 재생(replay)한다 (진짜 멱등).</li>
 *   <li>아직 처리 중인 키 → 409 (돌려줄 결과가 아직 없음 — 유일하게 정당한 409).</li>
 *   <li>저장 실패(길이 초과 등) → 마커를 회수해 재시도를 허용하고 원래 예외를 전파한다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterNotificationService implements RegisterNotificationUseCase {

    private final IdempotencyPort idempotencyPort;
    private final NotificationPersistencePort persistencePort;
    private final NotificationRegistrar registrar;

    @Override
    public NotificationResult register(String idempotencyKey, RegisterNotificationCommand command) {
        validate(command);

        // SETNX + TTL — 최초 요청만 선점에 성공한다.
        if (!idempotencyPort.tryClaim(idempotencyKey)) {
            return persistencePort.findByIdempotencyKey(idempotencyKey)
                    .map(existing -> {
                        log.info("멱등 재요청 — 기존 결과 재생 key={}, id={}", idempotencyKey, existing.getId());
                        return NotificationResult.from(existing);
                    })
                    .orElseThrow(() -> new DuplicateRequestException(
                            "처리 중인 요청입니다. 잠시 후 다시 시도해주세요: " + idempotencyKey));
        }

        try {
            return registrar.persist(idempotencyKey, command);
        } catch (RuntimeException e) {
            // 실패 원인을 추측하지 않는다 — "실제로 저장됐는가"만 확인한다.
            Optional<Notification> existing = persistencePort.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                // 행이 있다 = 진짜 중복 (마커 유실 후 경합, 유니크 제약 작동) → 마커 유지 + 재생
                log.info("DB 유니크 제약으로 중복 방지 — 기존 결과 재생 key={}", idempotencyKey);
                return NotificationResult.from(existing.get());
            }
            // 행이 없다 = 처리된 적 없음 (길이 초과 등 다른 실패) → 마커 회수 + 원래 예외 전파
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
        if (command.getRecipient().length() > 200) {
            throw new IllegalArgumentException("recipient 는 200자 이하여야 합니다.");
        }
        if (command.getMessage() == null || command.getMessage().isBlank()) {
            throw new IllegalArgumentException("message 는 필수입니다.");
        }
        if (command.getTitle() != null && command.getTitle().length() > 200) {
            throw new IllegalArgumentException("title 은 200자 이하여야 합니다.");
        }
    }
}
