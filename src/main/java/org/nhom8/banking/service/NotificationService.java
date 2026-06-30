package org.nhom8.banking.service;

import org.nhom8.banking.dto.response.NotificationResponse;
import org.nhom8.banking.entity.Notification;
import org.springframework.data.domain.Page;

public interface NotificationService {

    /** Đếm thông báo chưa đọc — type null = tất cả */
    long countUnread(Long userId, Notification.NotificationType type);

    /** Lấy danh sách thông báo — type null = tất cả */
    Page<NotificationResponse> getNotifications(Long userId, Notification.NotificationType type, int page, int size);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);
}
