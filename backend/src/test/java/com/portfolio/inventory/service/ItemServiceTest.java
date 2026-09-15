package com.portfolio.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio.inventory.domain.Item;
import com.portfolio.inventory.repository.ItemRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {
    @Mock ItemRepository itemRepository;

    @Test
    void trimsSearchTermAndMapsResults() {
        when(itemRepository
                .findTop20BySkuContainingIgnoreCaseOrNameContainingIgnoreCaseOrProductCategoryContainingIgnoreCaseOrderByNameAsc(
                        "scanner", "scanner", "scanner"))
                .thenReturn(List.of(new Item(1L, "ITM-1001", "Scanner",
                        "Scanning & Mobility", "Rugged", 42)));

        var results = new ItemService(itemRepository).search("  scanner  ");

        assertThat(results).singleElement().satisfies(item -> {
            assertThat(item.sku()).isEqualTo("ITM-1001");
            assertThat(item.productCategory()).isEqualTo("Scanning & Mobility");
            assertThat(item.availableQuantity()).isEqualTo(42);
        });
        verify(itemRepository)
                .findTop20BySkuContainingIgnoreCaseOrNameContainingIgnoreCaseOrProductCategoryContainingIgnoreCaseOrderByNameAsc(
                        "scanner", "scanner", "scanner");
    }
}
