package com.portfolio.inventory.repository;

import com.portfolio.inventory.domain.ReceiptLineSerial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptLineSerialRepository extends JpaRepository<ReceiptLineSerial, Long> {
    boolean existsBySerialNumberIgnoreCase(String serialNumber);
}
