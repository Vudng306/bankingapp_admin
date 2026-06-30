package org.nhom8.banking.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ── Authentication ────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng"),
    PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    USER_LOCKED(HttpStatus.FORBIDDEN, "Tài khoản người dùng đã bị khóa"),
    INVALID_OTP(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ hoặc đã hết hạn"),
    INVALID_PIN(HttpStatus.BAD_REQUEST, "Mã PIN không đúng"),
    PIN_NOT_SET(HttpStatus.BAD_REQUEST, "Chưa thiết lập mã PIN giao dịch"),

    // ── Account ───────────────────────────────────────────────────
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Tài khoản ngân hàng đã bị khóa"),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "Số dư tài khoản không đủ"),
    SAME_ACCOUNT_TRANSFER(HttpStatus.BAD_REQUEST, "Không thể chuyển tiền vào chính tài khoản đó"),
    ACCOUNT_NUMBER_EXISTS(HttpStatus.CONFLICT, "Số tài khoản đã tồn tại"),

    // ── Transaction ───────────────────────────────────────────────
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch"),
    DUPLICATE_REFERENCE_CODE(HttpStatus.CONFLICT, "Mã tham chiếu giao dịch đã tồn tại"),
    INVALID_BANK_CODE(HttpStatus.BAD_REQUEST, "Mã ngân hàng không hợp lệ hoặc không được hỗ trợ"),
    TRANSFER_SESSION_NOT_FOUND(HttpStatus.BAD_REQUEST, "Phiên xác thực giao dịch không hợp lệ hoặc đã hết hạn"),

    // ── Savings ───────────────────────────────────────────────────
    SAVINGS_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy sổ tiết kiệm"),
    SAVINGS_NOT_MATURED(HttpStatus.BAD_REQUEST, "Sổ tiết kiệm chưa đến ngày đáo hạn"),
    SAVINGS_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "Sổ tiết kiệm đã đóng"),
    SAVINGS_INVALID_TERM(HttpStatus.BAD_REQUEST, "Kỳ hạn không hợp lệ. Hỗ trợ: 1, 3, 6, 12, 24 tháng"),
    SAVINGS_MINIMUM_AMOUNT(HttpStatus.BAD_REQUEST, "Số tiền gửi tiết kiệm tối thiểu là 1,000,000 VND"),

    // ── Card ──────────────────────────────────────────────────────
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thẻ"),
    CARD_LIMIT_REACHED(HttpStatus.BAD_REQUEST, "Tài khoản đã đạt giới hạn số lượng thẻ (tối đa 5 thẻ)"),
    CARD_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo số thẻ, vui lòng thử lại"),

    // ── Phone Topup ───────────────────────────────────────────────
    INVALID_CARRIER(HttpStatus.BAD_REQUEST, "Nhà mạng không hợp lệ"),
    INVALID_FACE_VALUE(HttpStatus.BAD_REQUEST, "Mệnh giá không hợp lệ. Hỗ trợ: 10k, 20k, 50k, 100k, 200k, 500k"),
    INVALID_TOPUP_PHONE(HttpStatus.BAD_REQUEST, "Số điện thoại nạp không hợp lệ"),

    // ── General ───────────────────────────────────────────────────
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Chưa xác thực, vui lòng đăng nhập"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Không có quyền thực hiện thao tác này"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
