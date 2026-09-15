package com.portfolio.inventory.api.dto;

public record ReceiptLineResponse(Long itemId, String sku, String itemName, int quantity, String serialNumber) {}

