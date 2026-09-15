package com.portfolio.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipt_lines")
public class ReceiptLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id")
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    private int quantity;
    private String serialNumber;

    protected ReceiptLine() {}

    public ReceiptLine(Receipt receipt, Item item, int quantity, String serialNumber) {
        this.receipt = receipt;
        this.item = item;
        this.quantity = quantity;
        this.serialNumber = serialNumber;
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }
    public String getSerialNumber() { return serialNumber; }
}

