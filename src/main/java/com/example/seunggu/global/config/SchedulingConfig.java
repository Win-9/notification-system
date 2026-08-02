package com.example.seunggu.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄러 활성화 (Outbox 릴레이 폴링용).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
