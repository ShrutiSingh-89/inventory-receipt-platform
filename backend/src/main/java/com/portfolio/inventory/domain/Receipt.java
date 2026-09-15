package com.portfolio.inventory.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "receipts")
public class Receipt {
    @Id
    private UUID id;
    private String receiptNumber;
    private String supplierName;
    @Enumerated(EnumType.STRING)
    private ReceiptStatus status;
    private Instant createdAt;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ReceiptLine> lines = new ArrayList<>();

    protected Receipt() {}

    public Receipt(UUID id, String receiptNumber, String supplierName, ReceiptStatus status, Instant createdAt) {
        this.id = id;
        this.receiptNumber = receiptNumber;
        this.supplierName = supplierName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void addLine(Item item, int quantity, List<String> serialNumbers) {
        lines.add(new ReceiptLine(this, item, quantity, serialNumbers));
    }

    public UUID getId() { return id; }
    public String getReceiptNumber() { return receiptNumber; }
    public String getSupplierName() { return supplierName; }
    public ReceiptStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ReceiptLine> getLines() { return lines; }
}
