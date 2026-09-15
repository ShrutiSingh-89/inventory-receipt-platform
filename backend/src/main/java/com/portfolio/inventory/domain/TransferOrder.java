package com.portfolio.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transfer_orders")
public class TransferOrder {
    @Id
    private UUID id;
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_organization_id")
    private InventoryOrganization sourceOrganization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_organization_id")
    private InventoryOrganization destinationOrganization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    private int quantity;
    @Enumerated(EnumType.STRING)
    private TransferOrderStatus status;
    private String requestedBy;
    private LocalDate neededBy;
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private long version;

    protected TransferOrder() {}

    public TransferOrder(UUID id, String orderNumber, InventoryOrganization sourceOrganization,
                         InventoryOrganization destinationOrganization, Item item, int quantity,
                         TransferOrderStatus status, String requestedBy, LocalDate neededBy,
                         Instant createdAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.sourceOrganization = sourceOrganization;
        this.destinationOrganization = destinationOrganization;
        this.item = item;
        this.quantity = quantity;
        this.status = status;
        this.requestedBy = requestedBy;
        this.neededBy = neededBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void update(InventoryOrganization sourceOrganization,
                       InventoryOrganization destinationOrganization, Item item, int quantity,
                       TransferOrderStatus status, String requestedBy, LocalDate neededBy,
                       Instant updatedAt) {
        this.sourceOrganization = sourceOrganization;
        this.destinationOrganization = destinationOrganization;
        this.item = item;
        this.quantity = quantity;
        this.status = status;
        this.requestedBy = requestedBy;
        this.neededBy = neededBy;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public InventoryOrganization getSourceOrganization() { return sourceOrganization; }
    public InventoryOrganization getDestinationOrganization() { return destinationOrganization; }
    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }
    public TransferOrderStatus getStatus() { return status; }
    public String getRequestedBy() { return requestedBy; }
    public LocalDate getNeededBy() { return neededBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
