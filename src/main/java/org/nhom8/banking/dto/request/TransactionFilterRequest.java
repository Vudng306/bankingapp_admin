package org.nhom8.banking.dto.request;

import lombok.Data;
import org.nhom8.banking.entity.Transaction;

import java.time.LocalDate;

@Data
public class TransactionFilterRequest {
    private Long accountId;                        // bắt buộc — được set bởi controller
    private Transaction.TransactionType type;      // null = không lọc
    private Transaction.TransactionStatus status;  // null = không lọc
    private LocalDate fromDate;                    // null = không lọc (inclusive)
    private LocalDate toDate;                      // null = không lọc (inclusive)
    private String keyword;                        // null/blank = không lọc (description | referenceCode)
}
