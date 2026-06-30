package org.nhom8.banking.common;

import lombok.Getter;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;

/**
 * Danh sách ngân hàng nội địa được hỗ trợ.
 * Mã ngân hàng theo chuẩn Napas / NHNN Việt Nam.
 */
@Getter
public enum BankCode {

    VCB ("Vietcombank"),
    TCB ("Techcombank"),
    MB  ("MB Bank"),
    BIDV("BIDV"),
    VTB ("VietinBank"),
    ACB ("ACB"),
    VPB ("VPBank"),
    TPB ("TPBank"),
    STB ("Sacombank"),
    MSB ("MSB"),
    SHB ("SHB"),
    OCB ("OCB");

    private final String displayName;

    BankCode(String displayName) {
        this.displayName = displayName;
    }

    /** Parse từ string, ném INVALID_BANK_CODE nếu không khớp */
    public static BankCode fromCode(String code) {
        if (code == null) throw new AppException(ErrorCode.INVALID_BANK_CODE);
        try {
            return BankCode.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_BANK_CODE);
        }
    }
}
