package com.portfolio.inventory.api.dto;

public record ItemResponse(
        Long id,
        String sku,
        String name,
        String productCategory,
        String description,
        int availableQuantity) {}
