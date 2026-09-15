package com.portfolio.inventory.api;

import com.portfolio.inventory.api.dto.OrganizationResponse;
import com.portfolio.inventory.service.TransferOrderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory-organizations")
public class InventoryOrganizationController {
    private final TransferOrderService transferOrderService;

    public InventoryOrganizationController(TransferOrderService transferOrderService) {
        this.transferOrderService = transferOrderService;
    }

    @GetMapping
    public List<OrganizationResponse> list() {
        return transferOrderService.listOrganizations();
    }
}
