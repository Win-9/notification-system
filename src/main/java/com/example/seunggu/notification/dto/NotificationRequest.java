package com.example.seunggu.notification.dto;

import com.example.seunggu.notification.domain.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알림 등록 요청 (POST /notifications).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    private NotificationChannel channel;
    private String recipient;
    private String title;
    private String message;
}
