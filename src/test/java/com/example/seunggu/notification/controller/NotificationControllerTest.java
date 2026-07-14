package com.example.seunggu.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.seunggu.global.exception.DuplicateRequestException;
import com.example.seunggu.notification.domain.NotificationChannel;
import com.example.seunggu.notification.domain.NotificationStatus;
import com.example.seunggu.notification.dto.NotificationRequest;
import com.example.seunggu.notification.dto.NotificationResponse;
import com.example.seunggu.notification.service.NotificationService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private static final String BODY = """
            {"channel":"KAKAO","recipient":"010-1234-5678","title":"제목","message":"내용"}
            """;

    private NotificationResponse response(NotificationStatus status) {
        return new NotificationResponse(1L, NotificationChannel.KAKAO, "010-1234-5678", status);
    }

    @Test
    @DisplayName("정상 등록이면 202 Accepted 와 응답 바디를 반환한다")
    void register_accepted() throws Exception {
        given(notificationService.register(eq("req-1"), any(NotificationRequest.class)))
                .willReturn(response(NotificationStatus.PENDING));

        mockMvc.perform(post("/notifications")
                        .header("Idempotency-Key", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Idempotency-Key 헤더가 없으면 400 Bad Request")
    void register_missingHeader_badRequest() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("검증 실패(IllegalArgumentException)면 400 Bad Request")
    void register_invalid_badRequest() throws Exception {
        given(notificationService.register(any(), any()))
                .willThrow(new IllegalArgumentException("channel 은 필수입니다."));

        mockMvc.perform(post("/notifications")
                        .header("Idempotency-Key", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("중복 요청(DuplicateRequestException)이면 409 Conflict")
    void register_duplicate_conflict() throws Exception {
        given(notificationService.register(any(), any()))
                .willThrow(new DuplicateRequestException("이미 처리된 요청입니다."));

        mockMvc.perform(post("/notifications")
                        .header("Idempotency-Key", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("조회 성공이면 200 OK")
    void get_found() throws Exception {
        given(notificationService.find(1L)).willReturn(Optional.of(response(NotificationStatus.SENT)));

        mockMvc.perform(get("/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("없는 알림 조회면 404 Not Found")
    void get_notFound() throws Exception {
        given(notificationService.find(2L)).willReturn(Optional.empty());

        mockMvc.perform(get("/notifications/2"))
                .andExpect(status().isNotFound());
    }
}
