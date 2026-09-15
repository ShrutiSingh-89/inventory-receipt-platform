package com.portfolio.inventory.api;

import com.portfolio.inventory.api.dto.CreateTransferOrderRequest;
import com.portfolio.inventory.api.dto.TransferOrderResponse;
import com.portfolio.inventory.api.dto.UpdateTransferOrderRequest;
import com.portfolio.inventory.service.TransferOrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfer-orders")
public class TransferOrderController {
    private final TransferOrderService transferOrderService;

    public TransferOrderController(TransferOrderService transferOrderService) {
        this.transferOrderService = transferOrderService;
    }

    @GetMapping
    public List<TransferOrderResponse> list() {
        return transferOrderService.list();
    }

    @PostMapping
    public ResponseEntity<TransferOrderResponse> create(
            @Valid @RequestBody CreateTransferOrderRequest request) {
        TransferOrderResponse response = transferOrderService.create(request);
        return ResponseEntity.created(URI.create("/api/transfer-orders/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public TransferOrderResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody UpdateTransferOrderRequest request) {
        return transferOrderService.update(id, request);
    }
}
