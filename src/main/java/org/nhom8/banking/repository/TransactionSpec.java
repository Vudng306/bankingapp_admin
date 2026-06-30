package org.nhom8.banking.repository;

import org.nhom8.banking.dto.request.TransactionFilterRequest;
import org.nhom8.banking.entity.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * JPA Specification factory cho Transaction — dynamic query theo filter.
 * Mỗi predicate trả cb.conjunction() (luôn true) khi giá trị null,
 * nên an toàn dùng .and() mà không cần null-check bên ngoài.
 */
public final class TransactionSpec {

    private TransactionSpec() {}

    /** Tổng hợp toàn bộ filter từ request object */
    public static Specification<Transaction> build(TransactionFilterRequest f) {
        return belongsToAccount(f.getAccountId())
                .and(hasType(f.getType()))
                .and(hasStatus(f.getStatus()))
                .and(fromDate(f.getFromDate()))
                .and(toDate(f.getToDate()))
                .and(keyword(f.getKeyword()));
    }

    // ── Predicates ────────────────────────────────────────────────────────────

    /**
     * Giao dịch thuộc về account — từ hoặc đến.
     * Hibernate tối ưu root.get("fromAccount").get("id") thành FK column trực tiếp,
     * không sinh JOIN không cần thiết.
     */
    static Specification<Transaction> belongsToAccount(Long accountId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("fromAccount").get("id"), accountId),
                cb.equal(root.get("toAccount").get("id"), accountId)
        );
    }

    static Specification<Transaction> hasType(Transaction.TransactionType type) {
        return (root, query, cb) ->
                type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    static Specification<Transaction> hasStatus(Transaction.TransactionStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    /** >= fromDate (đầu ngày) */
    static Specification<Transaction> fromDate(LocalDate date) {
        return (root, query, cb) ->
                date == null ? cb.conjunction()
                             : cb.greaterThanOrEqualTo(root.get("createdAt"), date.atStartOfDay());
    }

    /** < toDate + 1 ngày (cuối ngày, inclusive) */
    static Specification<Transaction> toDate(LocalDate date) {
        return (root, query, cb) ->
                date == null ? cb.conjunction()
                             : cb.lessThan(root.get("createdAt"), date.plusDays(1).atStartOfDay());
    }

    /** LIKE không phân biệt hoa/thường trên description hoặc referenceCode */
    static Specification<Transaction> keyword(String kw) {
        return (root, query, cb) -> {
            if (kw == null || kw.isBlank()) return cb.conjunction();
            String pat = "%" + kw.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("description")), pat),
                    cb.like(cb.lower(root.get("referenceCode")), pat)
            );
        };
    }
}
