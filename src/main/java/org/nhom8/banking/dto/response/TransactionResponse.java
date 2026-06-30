package org.nhom8.banking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {

    private Long          id;
    private String        type;               // INTERNAL, INTERBANK, TOPUP, ...
    private String        status;             // PENDING, SUCCESS, FAILED
    private BigDecimal    amount;
    private BigDecimal    fee;
    /** DEBIT = tôi gửi đi | CREDIT = tôi nhận về */
    private String        direction;
    private String        counterpartAccount; // số tài khoản đối ứng
    private String        counterpartName;    // tên chủ tài khoản / tên người nhận liên ngân hàng
    private String        counterpartBank;    // mã ngân hàng (liên ngân hàng)
    private String        description;
    private String        referenceCode;
    private LocalDateTime createdAt;
}
