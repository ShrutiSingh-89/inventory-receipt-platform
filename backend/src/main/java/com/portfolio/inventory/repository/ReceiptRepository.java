package com.portfolio.inventory.repository;

import com.portfolio.inventory.domain.Receipt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    @EntityGraph(attributePaths = {"lines", "lines.item"})
    Optional<Receipt> findDetailedById(UUID id);
}

