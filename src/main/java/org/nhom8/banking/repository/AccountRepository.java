package org.nhom8.banking.repository;

import org.nhom8.banking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByIdAndUserId(Long id, Long userId);
    List<Account> findByUserId(Long userId);
    boolean existsByAccountNumber(String accountNumber);

    // ── Locking — chỉ dùng trong @Transactional khi cần sửa balance ──────────

    /**
     * Pessimistic write lock: native SELECT ... FOR UPDATE.
     * Dùng native query vì Hibernate 7.x sinh "FOR UPDATE OF alias"
     * không hợp lệ trên MySQL/MariaDB.
     * Luôn acquire lock theo thứ tự ID tăng dần để tránh deadlock.
     */
    @Query(value = "SELECT * FROM accounts WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
