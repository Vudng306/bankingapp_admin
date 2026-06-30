package org.nhom8.banking.repository;

import org.nhom8.banking.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReferenceCode(String referenceCode);
    boolean existsByReferenceCode(String referenceCode);

    /** Lịch sử giao dịch của một tài khoản — truyền cùng ID cho cả 2 tham số */
    Page<Transaction> findByFromAccount_IdOrToAccount_Id(
            Long fromAccountId, Long toAccountId, Pageable pageable);

    /** Giao dịch của nhiều tài khoản — truyền cùng list cho cả 2 tham số;
     *  DISTINCT để tránh duplicate khi chuyển nội bộ giữa 2 tài khoản cùng user */
    List<Transaction> findDistinctByFromAccount_IdInOrToAccount_IdIn(
            List<Long> fromAccountIds, List<Long> toAccountIds, Pageable pageable);

    // ── Spending report — GROUP BY month / week ───────────────────────────────

    /**
     * Thống kê thu/chi nhóm theo THÁNG.
     *
     * Logic phân loại:
     *   - Chi (expense): from_account_id thuộc user, to_account_id KHÔNG thuộc user
     *     → chuyển khoản ra ngoài, nạp tiết kiệm, chuyển liên ngân hàng
     *   - Thu (income) : to_account_id thuộc user, from_account_id KHÔNG thuộc user
     *     → nhận chuyển khoản, tất toán tiết kiệm, topup
     *   - Chuyển nội bộ giữa 2 tài khoản cùng user bị LOẠI TRỪ (không phải thu/chi thực)
     *
     * Điều kiện NULL: SAVINGS_DEPOSIT có to_account_id = NULL,
     *                 SAVINGS_WITHDRAW có from_account_id = NULL — đều được tính đúng.
     */
    @Query(value = """
            SELECT
                DATE_FORMAT(t.created_at, '%Y-%m')                                              AS label,
                COALESCE(SUM(CASE WHEN t.to_account_id   IN (:ids) THEN t.amount ELSE 0 END),0) AS income,
                COALESCE(SUM(CASE WHEN t.from_account_id IN (:ids) THEN t.amount ELSE 0 END),0) AS expense,
                COUNT(DISTINCT t.id)                                                             AS txCount
            FROM transactions t
            WHERE t.status = 'SUCCESS'
              AND t.created_at BETWEEN :from AND :to
              AND (
                    ( t.from_account_id IN (:ids)
                      AND (t.to_account_id IS NULL OR t.to_account_id NOT IN (:ids)) )
                 OR ( t.to_account_id   IN (:ids)
                      AND (t.from_account_id IS NULL OR t.from_account_id NOT IN (:ids)) )
              )
            GROUP BY DATE_FORMAT(t.created_at, '%Y-%m')
            ORDER BY DATE_FORMAT(t.created_at, '%Y-%m')
            """, nativeQuery = true)
    List<SpendingRowProjection> findMonthlySpending(
            @Param("ids")  List<Long> accountIds,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    /**
     * Thống kê thu/chi nhóm theo TUẦN (ISO week: thứ Hai là ngày đầu tuần).
     * Label format: "2024-W03" — sắp xếp đúng thứ tự thời gian khi ORDER BY string.
     */
    @Query(value = """
            SELECT
                DATE_FORMAT(t.created_at, '%x-W%v')                                             AS label,
                COALESCE(SUM(CASE WHEN t.to_account_id   IN (:ids) THEN t.amount ELSE 0 END),0) AS income,
                COALESCE(SUM(CASE WHEN t.from_account_id IN (:ids) THEN t.amount ELSE 0 END),0) AS expense,
                COUNT(DISTINCT t.id)                                                             AS txCount
            FROM transactions t
            WHERE t.status = 'SUCCESS'
              AND t.created_at BETWEEN :from AND :to
              AND (
                    ( t.from_account_id IN (:ids)
                      AND (t.to_account_id IS NULL OR t.to_account_id NOT IN (:ids)) )
                 OR ( t.to_account_id   IN (:ids)
                      AND (t.from_account_id IS NULL OR t.from_account_id NOT IN (:ids)) )
              )
            GROUP BY DATE_FORMAT(t.created_at, '%x-W%v')
            ORDER BY DATE_FORMAT(t.created_at, '%x-W%v')
            """, nativeQuery = true)
    List<SpendingRowProjection> findWeeklySpending(
            @Param("ids")  List<Long> accountIds,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    // ── Pie chart — GROUP BY transaction type ────────────────────────────────

    /** Chi (outflow) phân loại theo loại giao dịch — dùng cho biểu đồ tròn chi tiêu. */
    @Query(value = """
            SELECT
                t.type        AS txType,
                SUM(t.amount) AS totalAmount,
                COUNT(t.id)   AS txCount
            FROM transactions t
            WHERE t.status = 'SUCCESS'
              AND t.created_at BETWEEN :from AND :to
              AND t.from_account_id IN (:ids)
              AND (t.to_account_id IS NULL OR t.to_account_id NOT IN (:ids))
            GROUP BY t.type
            ORDER BY totalAmount DESC
            """, nativeQuery = true)
    List<TypeBreakdownProjection> findExpenseBreakdownByType(
            @Param("ids")  List<Long> accountIds,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    /** Thu (inflow) phân loại theo loại giao dịch — dùng cho biểu đồ tròn thu nhập. */
    @Query(value = """
            SELECT
                t.type        AS txType,
                SUM(t.amount) AS totalAmount,
                COUNT(t.id)   AS txCount
            FROM transactions t
            WHERE t.status = 'SUCCESS'
              AND t.created_at BETWEEN :from AND :to
              AND t.to_account_id IN (:ids)
              AND (t.from_account_id IS NULL OR t.from_account_id NOT IN (:ids))
            GROUP BY t.type
            ORDER BY totalAmount DESC
            """, nativeQuery = true)
    List<TypeBreakdownProjection> findIncomeBreakdownByType(
            @Param("ids")  List<Long> accountIds,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    // ── Bulk operations — cần @Query vì derived query không hỗ trợ UPDATE ──────

    /** Hủy toàn bộ giao dịch PENDING của tài khoản khi bị khóa/xử lý đặc biệt */
    @Modifying
    @Query("UPDATE Transaction t SET t.status = org.nhom8.banking.entity.Transaction.TransactionStatus.FAILED " +
           "WHERE t.fromAccount.id = :accountId " +
           "AND t.status = org.nhom8.banking.entity.Transaction.TransactionStatus.PENDING")
    int failPendingByFromAccountId(@Param("accountId") Long accountId);
}
