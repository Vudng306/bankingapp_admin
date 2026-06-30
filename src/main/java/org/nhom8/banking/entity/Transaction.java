package org.nhom8.banking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @Column(name = "to_external_account", length = 20)
    private String toExternalAccount;

    @Column(name = "to_external_account_name", length = 100)
    private String toExternalAccountName;

    @Column(name = "to_bank_code", length = 20)
    private String toBankCode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 255)
    private String description;

    @Column(name = "reference_code", unique = true, nullable = false, length = 30)
    private String referenceCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum TransactionType {
        INTERNAL, INTERBANK, TOPUP, SAVINGS_DEPOSIT, SAVINGS_WITHDRAW
    }

    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED
    }
}
