package org.nhom8.banking.service;

import org.nhom8.banking.dto.request.ConfirmTransferRequest;
import org.nhom8.banking.dto.request.TopupInitiateRequest;
import org.nhom8.banking.dto.response.CarrierResponse;
import org.nhom8.banking.dto.response.FaceValueResponse;
import org.nhom8.banking.dto.response.TopupInitiateResponse;
import org.nhom8.banking.dto.response.TopupReceiptResponse;

import java.util.List;

public interface PhoneTopupService {

    List<CarrierResponse>   getCarriers();
    List<FaceValueResponse> getFaceValues();

    TopupInitiateResponse initiateTopup(Long userId, TopupInitiateRequest request);

    TopupReceiptResponse confirmTopup(Long userId, ConfirmTransferRequest request);
}
