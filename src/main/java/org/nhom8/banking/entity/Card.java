package org.nhom8.banking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Card {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** 16 chữ số, sinh tự động, lưu không có khoảng trắng */
    @Column(name = "card_number", unique = true, nullable = false, length = 20)
    private String cardNumber;

    /** created_at + 3 năm */
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "cardholder_name", nullable = false, length = 100)
    private String cardholderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CardStatus status = CardStatus.ACTIVE;

    /** null = không giới hạn */
    @Column(name = "daily_limit", precision = 15, scale = 2)
    private BigDecimal dailyLimit;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum CardStatus { ACTIVE, LOCKED }
}
