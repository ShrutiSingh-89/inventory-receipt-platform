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
@Table(name = "receipt_line_serials")
public class ReceiptLineSerial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_line_id")
    private ReceiptLine receiptLine;

    private String serialNumber;
    private String assignmentSource;

    protected ReceiptLineSerial() {}

    public ReceiptLineSerial(ReceiptLine receiptLine, String serialNumber, String assignmentSource) {
        this.receiptLine = receiptLine;
        this.serialNumber = serialNumber;
        this.assignmentSource = assignmentSource;
    }

    public Long getId() { return id; }
    public String getSerialNumber() { return serialNumber; }
    public String getAssignmentSource() { return assignmentSource; }
}
