package com.example.seunggu.notification.adapter.out.sender.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MockSendRequest {
    private String requestId;
    private String channelType;
    private String receiver;
    private String message;
}
