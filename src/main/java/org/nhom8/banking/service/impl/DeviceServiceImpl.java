package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.dto.request.RegisterDeviceRequest;
import org.nhom8.banking.dto.response.DeviceResponse;
import org.nhom8.banking.entity.Device;
import org.nhom8.banking.entity.User;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.DeviceRepository;
import org.nhom8.banking.repository.UserRepository;
import org.nhom8.banking.service.DeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository   userRepository;

    /**
     * Upsert thiết bị theo (userId, deviceId).
     *
     * Nếu tìm thấy device cũ:
     *   - Cập nhật pushToken, deviceName, lastLoginAt, active = true
     * Nếu không tìm thấy:
     *   - Tạo mới
     *
     * Gọi sau mỗi lần đăng nhập để push token luôn fresh.
     * Một user có thể có nhiều thiết bị (multi-device).
     */
    @Override
    @Transactional
    public DeviceResponse registerOrUpdate(Long userId, RegisterDeviceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Device device = deviceRepository
                .findByUserIdAndDeviceId(userId, request.getDeviceId())
                .map(existing -> {
                    existing.setPushToken(request.getPushToken());
                    if (request.getDeviceName() != null) existing.setDeviceName(request.getDeviceName());
                    existing.setLastLoginAt(LocalDateTime.now());
                    existing.setActive(true);
                    return existing;
                })
                .orElseGet(() -> deviceRepository.save(Device.builder()
                        .user(user)
                        .deviceId(request.getDeviceId())
                        .deviceName(request.getDeviceName())
                        .pushToken(request.getPushToken())
                        .lastLoginAt(LocalDateTime.now())
                        .active(true)
                        .build()));

        log.info("Device upserted: userId={} deviceId={} pushEnabled={}",
                userId, request.getDeviceId(), device.getPushToken() != null);

        return toResponse(device);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> list(Long userId) {
        return deviceRepository.findByUserIdOrderByLastLoginAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void unregister(Long userId, Long deviceId) {
        Device device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        device.setActive(false);
        device.setPushToken(null);   // xoá token để không gửi FCM nữa
        log.info("Device unregistered: id={} userId={}", deviceId, userId);
    }

    /**
     * Dùng bởi JwtAuthenticationFilter — gọi trên mỗi request có claim "did".
     * readOnly=true để tránh xin write lock không cần thiết.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isDeviceActive(Long userId, String deviceId) {
        return deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .map(Device::isActive)
                .orElse(false); // không tìm thấy → coi như đã bị thu hồi
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DeviceResponse toResponse(Device d) {
        return DeviceResponse.builder()
                .id(d.getId())
                .deviceId(d.getDeviceId())
                .deviceName(d.getDeviceName())
                .pushEnabled(d.getPushToken() != null && !d.getPushToken().isBlank())
                .biometricEnabled(d.isBiometricEnabled())
                .active(d.isActive())
                .lastLoginAt(d.getLastLoginAt())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
