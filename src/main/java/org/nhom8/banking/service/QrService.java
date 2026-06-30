package org.nhom8.banking.service;

import org.nhom8.banking.dto.request.QrGenerateRequest;
import org.nhom8.banking.dto.response.QrResponse;

public interface QrService {

    /**
     * Sinh nội dung QR theo chuẩn VietQR cho tài khoản của user.
     *
     * @param userId  ID người dùng đang đăng nhập
     * @param request accountId bắt buộc; amount và description tuỳ chọn
     */
    QrResponse generate(Long userId, QrGenerateRequest request);
}
