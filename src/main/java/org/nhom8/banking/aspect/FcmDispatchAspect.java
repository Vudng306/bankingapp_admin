package org.nhom8.banking.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.nhom8.banking.entity.Device;
import org.nhom8.banking.entity.Notification;
import org.nhom8.banking.repository.DeviceRepository;
import org.nhom8.banking.service.FcmService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AOP aspect: tự động gửi FCM push notification sau mỗi lần Notification được lưu DB.
 *
 * Pointcut dùng bean() designator nên chỉ intercept đúng notificationRepository bean —
 * KHÔNG ảnh hưởng các repository khác dù chúng cũng có save().
 *
 * Không cần thay đổi bất kỳ service nào hiện có (TransferServiceImpl, SavingsServiceImpl…).
 * Advice chạy trong cùng transaction với save() — FCM dispatch là @Async nên không block.
 *
 * Edge case: nếu transaction rollback SAU khi advice đã chạy, FCM đã được gửi nhưng
 * notification không được persist. Cho dự án sinh viên điều này chấp nhận được.
 * Production: dùng Transactional Outbox Pattern.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FcmDispatchAspect {

    private final DeviceRepository deviceRepository;
    private final FcmService       fcmService;

    /**
     * Intercept notificationRepository.save(entity) — trả về Notification đơn lẻ.
     */
    @AfterReturning(
            pointcut = "bean(notificationRepository) && execution(* save(*))",
            returning = "result")
    public void afterSave(Object result) {
        if (result instanceof Notification n) dispatch(n);
    }

    /**
     * Intercept notificationRepository.saveAll(list) — trả về List<Notification>.
     * Dùng cho TransferServiceImpl.executeInternal() gọi saveAll(2 notifications).
     */
    @AfterReturning(
            pointcut = "bean(notificationRepository) && execution(* saveAll(*))",
            returning = "result")
    public void afterSaveAll(Object result) {
        if (!(result instanceof Iterable<?> items)) return;
        for (Object item : items) {
            if (item instanceof Notification n) dispatch(n);
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void dispatch(Notification notification) {
        Long userId = notification.getUser().getId();

        List<Device> devices = deviceRepository
                .findByUserIdAndActiveTrueAndPushTokenIsNotNull(userId);

        if (devices.isEmpty()) return;

        String title = notification.getTitle();
        String body  = notification.getContent();
        String type  = notification.getType().name();

        // FcmService.sendAsync() là @Async → gửi trên thread riêng, không block transaction
        devices.forEach(d -> fcmService.sendAsync(d.getPushToken(), title, body, type));

        log.debug("FCM dispatch: userId={} devices={} title='{}'", userId, devices.size(), title);
    }
}
