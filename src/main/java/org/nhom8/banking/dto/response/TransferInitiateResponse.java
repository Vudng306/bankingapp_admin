package org.nhom8.banking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferInitiateResponse {

    /** UUID — dùng trong bước confirm để xác định phiên giao dịch */
    private String confirmToken;

    /** Thông tin kênh OTP đã gửi (channel, target, expiresInSeconds) */
    private OtpResponse otpResponse;

    // ── Tóm tắt giao dịch để hiển thị trên màn hình xác nhận ──────────────────

    private String     fromAccountNumber;
    private String     fromAccountName;
    private String     fromBankName;

    private String     toAccountNumber;
    private String     toAccountName;
    private String     toBankName;

    private BigDecimal amount;
    private String     description;
}
