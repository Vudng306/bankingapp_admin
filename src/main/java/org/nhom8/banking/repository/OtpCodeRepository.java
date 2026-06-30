package org.nhom8.banking.repository;

import org.nhom8.banking.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            Long userId, OtpCode.OtpPurpose purpose);

    /** Hủy toàn bộ OTP chưa dùng của user theo purpose trước khi tạo mới */
    @Modifying
    @Query("UPDATE OtpCode o SET o.used = true " +
           "WHERE o.user.id = :userId AND o.purpose = :purpose AND o.used = false")
    void invalidatePrevious(@Param("userId") Long userId,
                            @Param("purpose") OtpCode.OtpPurpose purpose);
}
