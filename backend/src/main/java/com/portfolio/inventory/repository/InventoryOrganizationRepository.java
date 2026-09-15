package com.portfolio.inventory.repository;

import com.portfolio.inventory.domain.InventoryOrganization;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryOrganizationRepository extends JpaRepository<InventoryOrganization, Long> {
    List<InventoryOrganization> findAllByOrderByNameAsc();
}
