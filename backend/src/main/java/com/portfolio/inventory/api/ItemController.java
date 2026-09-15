package com.portfolio.inventory.api;

import com.portfolio.inventory.api.dto.ItemResponse;
import com.portfolio.inventory.service.ItemService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public List<ItemResponse> search(@RequestParam(defaultValue = "") String query) {
        return itemService.search(query);
    }
}

