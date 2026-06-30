package org.nhom8.banking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpResponse {

    /** "EMAIL" hoặc "SMS" */
    private String channel;

    /** Địa chỉ nhận OTP đã che bớt: "te***@x.com" / "091***5678" */
    private String target;

    /** Số giây còn hiệu lực */
    private int expiresInSeconds;

    /**
     * Chỉ xuất hiện khi app.otp.dev-mode=true.
     * Null trong production → bị @JsonInclude(NON_NULL) bỏ qua.
     */
    private String devOtpCode;
}
