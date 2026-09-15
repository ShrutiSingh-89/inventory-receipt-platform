package com.portfolio.inventory.api.dto;

import java.util.List;

public record ReceiptLineResponse(
        Long itemId,
        String sku,
        String itemName,
        int quantity,
        List<String> serialNumbers) {}
