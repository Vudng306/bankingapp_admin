package org.nhom8.banking.service;

import org.nhom8.banking.dto.request.UpdatePinRequest;
import org.nhom8.banking.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

    /** Lấy thông tin cá nhân + danh sách tài khoản của user */
    UserProfileResponse getProfile(Long userId);

    /**
     * Cập nhật PIN:
     * - Lần đầu (pinHash = null): không cần currentPin
     * - Đã có PIN: bắt buộc xác minh currentPin
     */
    void updatePin(Long userId, UpdatePinRequest request);

    /**
     * Upload ảnh đại diện (jpg/png, tối đa 2 MB).
     * Lưu file vào thư mục local, trả về URL truy cập.
     */
    String uploadAvatar(Long userId, MultipartFile file);
}
