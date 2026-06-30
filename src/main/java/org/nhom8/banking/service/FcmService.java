package org.nhom8.banking.service;

public interface FcmService {

    /**
     * Gửi push notification tới một FCM registration token.
     * Chạy bất đồng bộ — không block transaction gọi vào.
     *
     * @param token            FCM registration token của thiết bị
     * @param title            Tiêu đề notification
     * @param body             Nội dung notification
     * @param notificationType Loại notification (TRANSACTION / BALANCE / SYSTEM)
     */
    void sendAsync(String token, String title, String body, String notificationType);
}
