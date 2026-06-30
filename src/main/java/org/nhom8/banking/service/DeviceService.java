package org.nhom8.banking.service;

import org.nhom8.banking.dto.request.RegisterDeviceRequest;
import org.nhom8.banking.dto.response.DeviceResponse;

import java.util.List;

public interface DeviceService {

    /**
     * Đăng ký hoặc cập nhật thiết bị (upsert theo deviceId).
     * Gọi mỗi lần user đăng nhập để giữ push_token luôn mới nhất.
     */
    DeviceResponse registerOrUpdate(Long userId, RegisterDeviceRequest request);

    /** Danh sách tất cả thiết bị của user (kể cả đã deactivate). */
    List<DeviceResponse> list(Long userId);

    /** Vô hiệu hóa thiết bị — push token sẽ không nhận notification nữa. */
    void unregister(Long userId, Long deviceId);

    /**
     * Kiểm tra thiết bị còn active không.
     * Dùng bởi JwtAuthenticationFilter để enforce đăng xuất từ xa.
     * Trả về false nếu thiết bị không tồn tại hoặc đã bị deactivate.
     */
    boolean isDeviceActive(Long userId, String deviceId);
}
