package com.example.seunggu.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import com.example.seunggu.notification.dto.NotificationRequest;
import com.example.seunggu.notification.dto.NotificationResponse;
import com.example.seunggu.notification.repository.NotificationRepository;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private NotificationRegistrar registrar;
    @InjectMocks
    private NotificationService service;

    private NotificationRequest validRequest() {
        return new NotificationRequest(NotificationChannel.KAKAO, "010-1234-5678", "제목", "내용");
    }

    @SuppressWarnings("unchecked")
    private RBucket<String> stubMarker(boolean acquired) {
        RBucket<String> marker = org.mockito.Mockito.mock(RBucket.class);
        doReturn(marker).when(redissonClient).getBucket(anyString());
        when(marker.setIfAbsent(anyString(), any(Duration.class))).thenReturn(acquired);
        return marker;
    }

    @Test
    @DisplayName("처음 온 요청이면 Redis 선점 후 persist 를 호출하고 결과를 반환한다")
    void firstRequest_persists() {
        stubMarker(true);
        NotificationResponse expected =
                new NotificationResponse(1L, NotificationChannel.KAKAO, "010-1234-5678", NotificationStatus.PENDING);
        when(registrar.persist(eq("key-1"), any(NotificationRequest.class))).thenReturn(expected);

        NotificationResponse result = service.register("key-1", validRequest());

        assertThat(result).isEqualTo(expected);
        verify(registrar).persist(eq("key-1"), any(NotificationRequest.class));
    }

    @Test
    @DisplayName("이미 선점된 키(중복)면 DuplicateRequestException 을 던지고 persist 를 호출하지 않는다")
    void duplicateRequest_throws() {
        stubMarker(false);

        assertThatThrownBy(() -> service.register("key-1", validRequest()))
                .isInstanceOf(DuplicateRequestException.class);
        verify(registrar, never()).persist(anyString(), any(NotificationRequest.class));
    }

    @Test
    @DisplayName("persist 에서 유니크 제약 위반이면 DuplicateRequestException 으로 변환한다")
    void uniqueViolation_becomesDuplicate() {
        stubMarker(true);
        when(registrar.persist(anyString(), any(NotificationRequest.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.register("key-1", validRequest()))
                .isInstanceOf(DuplicateRequestException.class);
    }

    @Test
    @DisplayName("persist 에서 일반 예외가 나면 Redis 마커를 삭제하고 예외를 전파한다")
    void failure_releasesMarker() {
        RBucket<String> marker = stubMarker(true);
        when(registrar.persist(anyString(), any(NotificationRequest.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.register("key-1", validRequest()))
                .isInstanceOf(RuntimeException.class);
        verify(marker).delete();
    }

    @Test
    @DisplayName("channel 이 null 이면 검증 예외를 던지고 Redis 에 접근하지 않는다")
    void invalidChannel_failsBeforeRedis() {
        NotificationRequest bad = new NotificationRequest(null, "010-1234-5678", "제목", "내용");

        assertThatThrownBy(() -> service.register("key-1", bad))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(redissonClient, registrar);
    }

    @Test
    @DisplayName("recipient 가 비어 있으면 검증 예외를 던진다")
    void blankRecipient_fails() {
        NotificationRequest bad = new NotificationRequest(NotificationChannel.SMS, "  ", "제목", "내용");

        assertThatThrownBy(() -> service.register("key-1", bad))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
