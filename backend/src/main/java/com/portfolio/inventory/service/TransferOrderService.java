package com.portfolio.inventory.service;

import com.portfolio.inventory.api.dto.CreateTransferOrderRequest;
import com.portfolio.inventory.api.dto.OrganizationResponse;
import com.portfolio.inventory.api.dto.TransferOrderResponse;
import com.portfolio.inventory.api.dto.UpdateTransferOrderRequest;
import com.portfolio.inventory.domain.InventoryOrganization;
import com.portfolio.inventory.domain.Item;
import com.portfolio.inventory.domain.TransferOrder;
import com.portfolio.inventory.domain.TransferOrderStatus;
import com.portfolio.inventory.exception.BusinessValidationException;
import com.portfolio.inventory.exception.ResourceNotFoundException;
import com.portfolio.inventory.exception.StaleTransferOrderException;
import com.portfolio.inventory.repository.InventoryOrganizationRepository;
import com.portfolio.inventory.repository.ItemRepository;
import com.portfolio.inventory.repository.TransferOrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferOrderService {
    private static final DateTimeFormatter ORDER_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final TransferOrderRepository transferOrderRepository;
    private final InventoryOrganizationRepository organizationRepository;
    private final ItemRepository itemRepository;
    private final Clock clock;

    public TransferOrderService(TransferOrderRepository transferOrderRepository,
                                InventoryOrganizationRepository organizationRepository,
                                ItemRepository itemRepository) {
        this(transferOrderRepository, organizationRepository, itemRepository, Clock.systemUTC());
    }

    TransferOrderService(TransferOrderRepository transferOrderRepository,
                         InventoryOrganizationRepository organizationRepository,
                         ItemRepository itemRepository, Clock clock) {
        this.transferOrderRepository = transferOrderRepository;
        this.organizationRepository = organizationRepository;
        this.itemRepository = itemRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizations() {
        return organizationRepository.findAllByOrderByNameAsc().stream()
                .map(organization -> new OrganizationResponse(organization.getId(), organization.getCode(),
                        organization.getName(), organization.getLocation()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferOrderResponse> list() {
        return transferOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TransferOrderResponse create(CreateTransferOrderRequest request) {
        InventoryOrganization source = findOrganization(request.sourceOrganizationId());
        InventoryOrganization destination = findOrganization(request.destinationOrganizationId());
        Item item = findItem(request.itemId());
        validateRequest(source, destination, item, request.quantity(), request.neededBy());

        Instant now = clock.instant();
        UUID id = UUID.randomUUID();
        TransferOrder order = new TransferOrder(id,
                "TO-" + ORDER_TIME.format(now) + "-" + id.toString().substring(0, 4).toUpperCase(Locale.ROOT),
                source, destination, item, request.quantity(), TransferOrderStatus.REQUESTED,
                request.requestedBy().trim(), request.neededBy(), now);
        return toResponse(transferOrderRepository.saveAndFlush(order));
    }

    @Transactional
    public TransferOrderResponse update(UUID id, UpdateTransferOrderRequest request) {
        TransferOrder order = transferOrderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer order " + id + " was not found"));
        if (order.getVersion() != request.version()) {
            throw new StaleTransferOrderException(
                    "This transfer order was changed by another user. Refresh the table and try again.");
        }

        InventoryOrganization source = findOrganization(request.sourceOrganizationId());
        InventoryOrganization destination = findOrganization(request.destinationOrganizationId());
        Item item = findItem(request.itemId());
        validateRequest(source, destination, item, request.quantity(), request.neededBy());
        order.update(source, destination, item, request.quantity(), request.status(),
                request.requestedBy().trim(), request.neededBy(), clock.instant());
        return toResponse(transferOrderRepository.saveAndFlush(order));
    }

    private InventoryOrganization findOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory organization " + id + " was not found"));
    }

    private Item findItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item " + id + " was not found"));
    }

    private void validateRequest(InventoryOrganization source, InventoryOrganization destination,
                                 Item item, int quantity, LocalDate neededBy) {
        if (source.getId().equals(destination.getId())) {
            throw new BusinessValidationException("Source and destination organizations must be different");
        }
        if (quantity > item.getAvailableQuantity()) {
            throw new BusinessValidationException("Requested quantity exceeds available inventory for " + item.getSku());
        }
        if (neededBy.isBefore(LocalDate.now(clock))) {
            throw new BusinessValidationException("Needed-by date cannot be in the past");
        }
    }

    private TransferOrderResponse toResponse(TransferOrder order) {
        InventoryOrganization source = order.getSourceOrganization();
        InventoryOrganization destination = order.getDestinationOrganization();
        Item item = order.getItem();
        return new TransferOrderResponse(order.getId(), order.getOrderNumber(), source.getId(), source.getCode(),
                source.getName(), destination.getId(), destination.getCode(), destination.getName(), item.getId(),
                item.getSku(), item.getName(), order.getQuantity(), order.getStatus(), order.getRequestedBy(),
                order.getNeededBy(), order.getCreatedAt(), order.getUpdatedAt(), order.getVersion());
    }
}
