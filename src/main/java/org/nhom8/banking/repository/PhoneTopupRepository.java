package org.nhom8.banking.repository;

import org.nhom8.banking.entity.PhoneTopup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhoneTopupRepository extends JpaRepository<PhoneTopup, Long> {
    Optional<PhoneTopup> findByTransactionId(Long transactionId);
}
