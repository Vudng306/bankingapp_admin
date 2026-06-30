package org.nhom8.banking.service;

import org.nhom8.banking.dto.response.OtpResponse;
import org.nhom8.banking.entity.OtpCode;
import org.nhom8.banking.entity.User;

public interface OtpService {

    /**
     * Sinh OTP mới, hủy các OTP cũ chưa dùng cùng purpose,
     * ghi log (và trả code trong response khi dev-mode=true).
     */
    OtpResponse generate(User user, OtpCode.OtpPurpose purpose, OtpCode.OtpChannel channel);

    /**
     * Xác thực OTP: kiểm tra code đúng, chưa dùng, chưa hết hạn.
     * Đánh dấu is_used=true nếu hợp lệ; ném AppException nếu không hợp lệ.
     */
    void verify(User user, String code, OtpCode.OtpPurpose purpose);
}
