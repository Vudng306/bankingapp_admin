package org.nhom8.banking.service;

import java.math.BigDecimal;

/**
 * Giả lập cổng liên ngân hàng.
 * dispatch() chạy bất đồng bộ; settle() được gọi qua self-proxy để @Transactional hoạt động.
 */
public interface InterbankGatewayService {

    /** Gửi lệnh chuyển tiền — trả về ngay, xử lý ngầm trong background thread */
    void dispatch(Long transactionId, Long fromAccountId, BigDecimal amount);

    /** Cập nhật kết quả sau khi xử lý xong (SUCCESS hoặc FAILED + hoàn tiền) */
    void settle(Long transactionId, Long fromAccountId, BigDecimal amount, boolean success);
}
