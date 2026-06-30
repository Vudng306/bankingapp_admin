package org.nhom8.banking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID không đoán được — làm token xác nhận giao dịch */
    @Column(name = "confirm_token", unique = true, nullable = false, length = 36)
    private String confirmToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** INTERNAL hoặc INTERBANK */
    @Column(name = "transfer_type", nullable = false, length = 20)
    private String transferType;

    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account_number", nullable = false, length = 20)
    private String toAccountNumber;

    /** Tên chủ tài khoản đích — chỉ dùng cho INTERBANK */
    @Column(name = "to_account_name", length = 100)
    private String toAccountName;

    /** Mã ngân hàng đích — chỉ dùng cho INTERBANK */
    @Column(name = "to_bank_code", length = 20)
    private String toBankCode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** true sau khi giao dịch đã được thực thi — ngăn dùng lại session */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
