package com.portfolio.inventory.service;

import com.portfolio.inventory.api.dto.ItemResponse;
import com.portfolio.inventory.repository.ItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> search(String query) {
        String normalized = query == null ? "" : query.trim();
        return itemRepository
                .findTop20BySkuContainingIgnoreCaseOrNameContainingIgnoreCaseOrProductCategoryContainingIgnoreCaseOrderByNameAsc(
                        normalized, normalized, normalized)
                .stream()
                .map(item -> new ItemResponse(item.getId(), item.getSku(), item.getName(), item.getProductCategory(),
                        item.getDescription(), item.getAvailableQuantity()))
                .toList();
    }
}
