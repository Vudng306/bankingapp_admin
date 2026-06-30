package org.nhom8.banking.service;

import org.nhom8.banking.dto.request.ConfirmTransferRequest;
import org.nhom8.banking.dto.request.InternalTransferRequest;
import org.nhom8.banking.dto.request.InterbankTransferRequest;
import org.nhom8.banking.dto.response.AccountLookupResponse;
import org.nhom8.banking.dto.response.TransactionResponse;
import org.nhom8.banking.dto.response.TransferInitiateResponse;

public interface TransferService {

    /**
     * Bước 1: Xác thực PIN + validate thông tin → lưu phiên → gửi OTP.
     * Chưa thực hiện chuyển tiền, chưa trừ số dư.
     */
    TransferInitiateResponse initiateInternal(Long userId, InternalTransferRequest request);

    TransferInitiateResponse initiateInterbank(Long userId, InterbankTransferRequest request);

    /**
     * Bước 2: Xác thực OTP → load phiên → thực thi giao dịch.
     * Áp dụng cho cả nội bộ và liên ngân hàng.
     */
    TransactionResponse confirmTransfer(Long userId, ConfirmTransferRequest request);

    /** Tra cứu tài khoản nội bộ theo số tài khoản — dùng trước khi nhập lệnh chuyển tiền. */
    AccountLookupResponse lookupAccount(String accountNumber);
}
