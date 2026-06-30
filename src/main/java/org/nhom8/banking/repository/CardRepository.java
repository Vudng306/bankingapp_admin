package org.nhom8.banking.repository;

import org.nhom8.banking.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    /** Tất cả thẻ của user (qua account.user), mới nhất trước */
    List<Card> findByAccountUserIdOrderByCreatedAtDesc(Long userId);

    /** Tìm thẻ theo id và kiểm tra ownership trong 1 query */
    Optional<Card> findByIdAndAccountUserId(Long id, Long userId);

    boolean existsByCardNumber(String cardNumber);

    int countByAccountId(Long accountId);
}
