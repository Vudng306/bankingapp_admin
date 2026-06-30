package org.nhom8.banking.service;

import org.nhom8.banking.dto.request.CreateCardRequest;
import org.nhom8.banking.dto.request.SetCardLimitRequest;
import org.nhom8.banking.dto.response.CardResponse;

import java.util.List;

public interface CardService {

    CardResponse       createCard(Long userId, CreateCardRequest request);
    List<CardResponse> getCards(Long userId);
    CardResponse       toggleLock(Long userId, Long cardId);
    CardResponse       setLimit(Long userId, Long cardId, SetCardLimitRequest request);
}
