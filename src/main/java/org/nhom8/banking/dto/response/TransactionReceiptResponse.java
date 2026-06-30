package org.nhom8.banking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionReceiptResponse {

    private Long          id;
    private String        referenceCode;
    private String        type;            // INTERNAL, INTERBANK, ...
    private String        status;          // SUCCESS, FAILED, PENDING

    // ── Bên gửi ──────────────────────────────────────────────────────────────
    private String        fromAccountNumber;
    private String        fromAccountName;

    // ── Bên nhận ─────────────────────────────────────────────────────────────
    private String        toAccountNumber;
    private String        toAccountName;
    private String        toBankCode;      // null nếu nội bộ
    private String        toBankName;      // tên đầy đủ ngân hàng đích

    // ── Số tiền ──────────────────────────────────────────────────────────────
    private BigDecimal    amount;
    private BigDecimal    fee;
    private BigDecimal    totalDeducted;   // amount + fee

    private String        description;
    private LocalDateTime createdAt;
}
