package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.dto.response.NotificationResponse;
import org.nhom8.banking.entity.Notification;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.NotificationRepository;
import org.nhom8.banking.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId, Notification.NotificationType type) {
        return type == null
                ? notificationRepository.countByUserIdAndReadFalse(userId)
                : notificationRepository.countByUserIdAndReadFalseAndType(userId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Notification.NotificationType type, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50));
        return (type == null
                ? notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!n.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        n.setRead(true);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    // -------------------------------------------------------------------------

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .type(n.getType().name())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
