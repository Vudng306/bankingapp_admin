package org.nhom8.banking.service;

import org.nhom8.banking.dto.request.TransactionFilterRequest;
import org.nhom8.banking.dto.response.TransactionReceiptResponse;
import org.nhom8.banking.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    /** Biên lai chi tiết — chỉ truy cập được nếu user là bên gửi hoặc bên nhận */
    TransactionReceiptResponse getReceipt(Long userId, Long transactionId);

    /**
     * Lịch sử giao dịch của một tài khoản — phân trang + lọc động.
     * filter.accountId bắt buộc; type, status, fromDate, toDate, keyword đều optional.
     */
    Page<TransactionResponse> getHistory(Long userId, TransactionFilterRequest filter, Pageable pageable);
}
