package org.nhom8.banking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TopupInitiateResponse {

    private String      confirmToken;
    private OtpResponse otpResponse;

    private String      fromAccountNumber;
    private String      fromAccountName;

    private String      phoneNumber;
    private String      carrier;
    private String      carrierName;
    private BigDecimal  faceValue;
    private String      faceValueLabel;
}
