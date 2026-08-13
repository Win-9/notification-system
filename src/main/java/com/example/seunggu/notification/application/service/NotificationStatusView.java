package com.example.seunggu.notification.application.service;

import com.example.seunggu.notification.domain.NotificationStatus;

public enum NotificationStatusView {
    PENDING,
    SUCCESS,
    FAIL

    ;

    public static NotificationStatusView changeStatusFromDb(NotificationStatus status) {
        if (status.equals(NotificationStatus.SENT)) {
            return SUCCESS;
        } else if (status.equals(NotificationStatus.FAILED) || status.equals(NotificationStatus.DEAD)) {
            return FAIL;
        } else {
            return PENDING;
        }
    }

}
