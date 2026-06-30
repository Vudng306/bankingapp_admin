package org.nhom8.banking.repository;

import org.nhom8.banking.entity.Savings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SavingsRepository extends JpaRepository<Savings, Long> {

    List<Savings> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Savings> findByIdAndUserId(Long id, Long userId);

    List<Savings> findByStatus(Savings.SavingsStatus status);

    /** Dùng cho scheduled job: ACTIVE savings đã qua ngày đáo hạn */
    List<Savings> findByStatusAndMaturityDateLessThanEqual(
            Savings.SavingsStatus status, LocalDate date);
}
