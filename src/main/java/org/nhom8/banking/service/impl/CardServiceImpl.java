package org.nhom8.banking.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.dto.request.CreateCardRequest;
import org.nhom8.banking.dto.request.SetCardLimitRequest;
import org.nhom8.banking.dto.response.CardResponse;
import org.nhom8.banking.entity.Account;
import org.nhom8.banking.entity.Card;
import org.nhom8.banking.exception.AppException;
import org.nhom8.banking.exception.ErrorCode;
import org.nhom8.banking.repository.AccountRepository;
import org.nhom8.banking.repository.CardRepository;
import org.nhom8.banking.service.CardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private static final int    CARD_EXPIRY_YEARS = 3;
    private static final int    MAX_CARDS_PER_ACCOUNT = 5;
    private static final String EXPIRY_PATTERN    = "MM/yy";

    private final CardRepository    cardRepository;
    private final AccountRepository accountRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // Tạo thẻ ảo
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public CardResponse createCard(Long userId, CreateCardRequest req) {
        Account account = accountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!account.getUser().getId().equals(userId))
            throw new AppException(ErrorCode.FORBIDDEN);

        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);

        if (cardRepository.countByAccountId(req.getAccountId()) >= MAX_CARDS_PER_ACCOUNT)
            throw new AppException(ErrorCode.CARD_LIMIT_REACHED);

        Card card = Card.builder()
                .account(account)
                .cardNumber(generateCardNumber())
                .expiryDate(LocalDate.now().plusYears(CARD_EXPIRY_YEARS))
                .cardholderName(account.getUser().getFullName().toUpperCase())
                .build();

        cardRepository.save(card);

        log.info("Card created id={} account={} userId={}", card.getId(), req.getAccountId(), userId);
        return toResponse(card);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Danh sách thẻ
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getCards(Long userId) {
        return cardRepository.findByAccountUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Khóa / Mở khóa thẻ
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public CardResponse toggleLock(Long userId, Long cardId) {
        Card card = findOwnedCard(userId, cardId);

        card.setStatus(card.getStatus() == Card.CardStatus.ACTIVE
                ? Card.CardStatus.LOCKED
                : Card.CardStatus.ACTIVE);

        log.info("Card {} toggled to {} userId={}", cardId, card.getStatus(), userId);
        return toResponse(card);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Thiết lập hạn mức hàng ngày
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public CardResponse setLimit(Long userId, Long cardId, SetCardLimitRequest req) {
        Card card = findOwnedCard(userId, cardId);
        card.setDailyLimit(req.getDailyLimit()); // null = bỏ hạn mức

        log.info("Card {} limit set to {} userId={}", cardId, req.getDailyLimit(), userId);
        return toResponse(card);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private Card findOwnedCard(Long userId, Long cardId) {
        return cardRepository.findByIdAndAccountUserId(cardId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.CARD_NOT_FOUND));
    }

    /**
     * Sinh số thẻ 16 chữ số, tiền tố "4" (Visa-style).
     * Thử tối đa 10 lần để đảm bảo unique, nếu thất bại ném lỗi hệ thống.
     */
    private String generateCardNumber() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("4");
            for (int i = 0; i < 15; i++) sb.append(rng.nextInt(10));
            String number = sb.toString();
            if (!cardRepository.existsByCardNumber(number)) return number;
        }
        throw new AppException(ErrorCode.CARD_GENERATION_FAILED);
    }

    /** "4123456789012345" → "**** **** **** 2345" */
    private String mask(String cardNumber) {
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    private CardResponse toResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .maskedNumber(mask(card.getCardNumber()))
                .expiryDate(card.getExpiryDate().format(DateTimeFormatter.ofPattern(EXPIRY_PATTERN)))
                .cardholderName(card.getCardholderName())
                .status(card.getStatus().name())
                .dailyLimit(card.getDailyLimit())
                .accountNumber(card.getAccount().getAccountNumber())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
