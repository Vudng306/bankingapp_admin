package org.nhom8.banking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardResponse {

    private Long          id;
    /** Số thẻ hiển thị dạng masked: "**** **** **** 1234" */
    private String        maskedNumber;
    /** Ngày hết hạn định dạng MM/yy, ví dụ "06/29" */
    private String        expiryDate;
    private String        cardholderName;
    private String        status;
    /** null nếu không giới hạn */
    private BigDecimal    dailyLimit;
    private String        accountNumber;
    private LocalDateTime createdAt;
}
