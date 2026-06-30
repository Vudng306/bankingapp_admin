package org.nhom8.banking.service.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.service.FcmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Gửi Firebase Cloud Messaging push notification.
 *
 * Cấu hình qua biến môi trường:
 *   FCM_ENABLED=true
 *   FCM_SERVICE_ACCOUNT_KEY=/path/to/service-account.json
 *                         hoặc nội dung JSON trực tiếp (cho Docker/K8s secrets)
 *
 * Khi FCM_ENABLED=false (mặc định), tất cả send() bị bỏ qua — không crash server.
 */
@Slf4j
@Service
public class FcmServiceImpl implements FcmService {

    @Value("${app.fcm.enabled:false}")
    private boolean enabled;

    @Value("${app.fcm.service-account-key:}")
    private String serviceAccountKey;

    /** null nếu FCM chưa được khởi tạo (dev-mode hoặc thiếu config) */
    private FirebaseMessaging messaging;

    @PostConstruct
    void init() {
        if (!enabled || serviceAccountKey.isBlank()) {
            log.info("FCM: push notification tắt (FCM_ENABLED=false hoặc thiếu service-account-key)");
            return;
        }
        try {
            InputStream keyStream = serviceAccountKey.trim().startsWith("{")
                    // Nội dung JSON trực tiếp (K8s secret, Docker env)
                    ? new ByteArrayInputStream(serviceAccountKey.getBytes(StandardCharsets.UTF_8))
                    // Đường dẫn đến file
                    : new FileInputStream(serviceAccountKey);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(keyStream))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            this.messaging = FirebaseMessaging.getInstance();
            log.info("FCM: khởi tạo thành công");

        } catch (Exception e) {
            log.error("FCM: khởi tạo thất bại — push notification sẽ bị bỏ qua: {}", e.getMessage());
        }
    }

    /**
     * Gửi push notification bất đồng bộ tới một token.
     *
     * Dùng lại "emailExecutor" (bounded thread pool) — tránh tạo thread vô giới hạn.
     * Firebase SDK tự retry nội bộ cho lỗi transient.
     *
     * Lỗi token stale/invalid được log ở WARN — caller không bị ảnh hưởng.
     */
    @Override
    @Async("emailExecutor")
    public void sendAsync(String token, String title, String body, String notificationType) {
        if (messaging == null || token == null || token.isBlank()) return;

        try {
            // Dùng fully-qualified vì tên trùng với entity Notification của project
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", notificationType)
                    .build();

            String messageId = messaging.send(message);
            log.debug("FCM sent: id={} token={}…", messageId,
                    token.substring(0, Math.min(12, token.length())));

        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                // Token hết hạn hoặc không hợp lệ — app Android nên cập nhật token
                log.warn("FCM: token stale/invalid ({}), app nên gọi lại POST /devices",
                        e.getMessagingErrorCode());
            } else {
                log.error("FCM: gửi thất bại — code={} msg={}",
                        e.getMessagingErrorCode(), e.getMessage());
            }
        }
    }
}
