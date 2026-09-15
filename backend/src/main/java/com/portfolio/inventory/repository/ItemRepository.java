package com.portfolio.inventory.repository;

import com.portfolio.inventory.domain.Item;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findTop20BySkuContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByNameAsc(String sku, String name);
}

