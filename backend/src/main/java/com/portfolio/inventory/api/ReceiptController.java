package com.portfolio.inventory.api;

import com.portfolio.inventory.api.dto.CreateReceiptRequest;
import com.portfolio.inventory.api.dto.ReceiptResponse;
import com.portfolio.inventory.service.ReceiptService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {
    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping
    public ResponseEntity<ReceiptResponse> create(@Valid @RequestBody CreateReceiptRequest request) {
        ReceiptResponse response = receiptService.create(request);
        return ResponseEntity.created(URI.create("/api/receipts/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ReceiptResponse get(@PathVariable UUID id) {
        return receiptService.get(id);
    }
}

