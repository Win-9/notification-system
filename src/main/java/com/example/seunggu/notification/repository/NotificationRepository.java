package com.example.seunggu.notification.repository;

import com.example.seunggu.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByIdempotencyKey(String idempotencyKey);
}
