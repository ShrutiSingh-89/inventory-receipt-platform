package com.portfolio.inventory.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "receiptLine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ReceiptLineSerial> serialNumbers = new ArrayList<>();

    protected ReceiptLine() {}

    public ReceiptLine(Receipt receipt, Item item, int quantity, List<String> serialNumbers) {
        this.receipt = receipt;
        this.item = item;
        this.quantity = quantity;
        serialNumbers.forEach(serial -> this.serialNumbers.add(
                new ReceiptLineSerial(this, serial, "USER_CONFIRMED")));
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }
    public List<ReceiptLineSerial> getSerialNumbers() { return serialNumbers; }
}
