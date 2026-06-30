package org.nhom8.banking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QrResponse {

    /** Chuỗi nội dung QR theo chuẩn VietQR/EMVCo — dùng để tự render ảnh QR (ZXing) */
    private String qrContent;

    /** Data URI ảnh PNG base64 do VietQR API trả về — có thể dùng trực tiếp không cần render */
    private String qrDataURL;

    private String accountNumber;
    private String accountName;
    private String bankBin;
    private String bankName;

    /** null nếu QR không gắn số tiền cố định */
    private BigDecimal amount;

    /** null nếu không có nội dung chuyển khoản */
    private String description;
}
