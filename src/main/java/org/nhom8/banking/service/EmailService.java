package org.nhom8.banking.service;

import org.nhom8.banking.entity.OtpCode;

public interface EmailService {

    /**
     * Gửi email OTP bất đồng bộ.
     * Lỗi SMTP được log, không ném ra ngoài để tránh rollback transaction.
     */
    void sendOtpEmail(String toEmail, String toName,
                      String otpCode, OtpCode.OtpPurpose purpose,
                      int expiryMinutes);
}
