package org.nhom8.banking.repository;

import org.nhom8.banking.entity.TransferSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TransferSessionRepository extends JpaRepository<TransferSession, Long> {

    Optional<TransferSession> findByConfirmToken(String confirmToken);

    /**
     * Đánh dấu phiên đã dùng một cách nguyên tử: chỉ cập nhật khi used = 0.
     * Trả về số hàng thực sự cập nhật — 0 nghĩa là phiên đã bị dùng hoặc không tồn tại.
     * UPDATE tự động giữ row-level lock trên MariaDB, ngăn double-spend đồng thời.
     */
    @Modifying
    @Query("UPDATE TransferSession ts SET ts.used = true " +
           "WHERE ts.confirmToken = :token AND ts.used = false")
    int markUsed(@Param("token") String token);

    /** Dọn dẹp phiên hết hạn — chạy định kỳ bởi @Scheduled */
    @Modifying
    @Transactional
    @Query("DELETE FROM TransferSession ts WHERE ts.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
