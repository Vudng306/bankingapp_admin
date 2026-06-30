package org.nhom8.banking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopupReceiptResponse {

    private Long          id;
    private String        referenceCode;
    private String        status;

    private String        fromAccountNumber;
    private String        fromAccountName;

    private String        phoneNumber;
    private String        carrier;
    private String        carrierName;
    private BigDecimal    faceValue;
    private String        faceValueLabel;

    private String        description;
    private LocalDateTime createdAt;
}
