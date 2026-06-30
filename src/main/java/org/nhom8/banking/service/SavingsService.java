package org.nhom8.banking.service;

import org.nhom8.banking.dto.request.OpenSavingsRequest;
import org.nhom8.banking.dto.request.WithdrawSavingsRequest;
import org.nhom8.banking.dto.response.SavingsResponse;
import org.nhom8.banking.dto.response.WithdrawSavingsResponse;

import java.util.List;

public interface SavingsService {

    /** Mở sổ: PIN → lock account → trích tiền → tạo Savings + Transaction. */
    SavingsResponse open(Long userId, OpenSavingsRequest request);

    /**
     * Tất toán sổ tiết kiệm.
     * - Đúng/quá hạn: hoàn principal + accruedInterest (lãi đủ hạn đã lưu).
     * - Rút sớm: hoàn principal + interest tính theo lãi suất không kỳ hạn × ngày thực gửi.
     */
    WithdrawSavingsResponse withdraw(Long userId, Long savingsId, WithdrawSavingsRequest request);

    /** Danh sách sổ của user, mới nhất trước. */
    List<SavingsResponse> list(Long userId);

    /** Chi tiết một sổ (phải thuộc user). */
    SavingsResponse detail(Long userId, Long savingsId);
}
