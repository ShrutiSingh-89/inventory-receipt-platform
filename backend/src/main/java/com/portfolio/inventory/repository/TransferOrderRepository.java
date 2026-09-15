package com.portfolio.inventory.repository;

import com.portfolio.inventory.domain.TransferOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferOrderRepository extends JpaRepository<TransferOrder, UUID> {
    @EntityGraph(attributePaths = {"sourceOrganization", "destinationOrganization", "item"})
    List<TransferOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"sourceOrganization", "destinationOrganization", "item"})
    Optional<TransferOrder> findDetailedById(UUID id);
}
