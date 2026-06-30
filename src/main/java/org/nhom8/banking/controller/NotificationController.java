package org.nhom8.banking.controller;

import lombok.RequiredArgsConstructor;
import org.nhom8.banking.common.ApiResponse;
import org.nhom8.banking.dto.response.NotificationResponse;
import org.nhom8.banking.entity.Notification;
import org.nhom8.banking.security.CustomUserDetails;
import org.nhom8.banking.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Số thông báo chưa đọc — dùng hiển thị badge.
     * type=SYSTEM      → chỉ đếm thông báo hệ thống (bell icon).
     * type=TRANSACTION → chỉ đếm biến động số dư.
     * Không truyền     → đếm tất cả.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Notification.NotificationType type) {
        long count = notificationService.countUnread(user.getId(), type);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    /**
     * Danh sách thông báo có phân trang.
     * type=SYSTEM      → tab "Thông báo" (tin hệ thống, khuyến mãi...).
     * type=TRANSACTION → tab "Biến động số dư" (chuyển tiền, nhận tiền...).
     * Không truyền     → tất cả.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) Notification.NotificationType type,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getNotifications(user.getId(), type, page, size)));
    }

    /** Đánh dấu một thông báo đã đọc */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        notificationService.markAsRead(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu đã đọc"));
    }

    /** Đánh dấu tất cả thông báo đã đọc */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu tất cả đã đọc"));
    }
}
