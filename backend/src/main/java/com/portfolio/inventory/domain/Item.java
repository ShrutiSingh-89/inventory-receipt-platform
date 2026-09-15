package com.portfolio.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sku;
    private String name;
    private String description;
    private int availableQuantity;

    protected Item() {}

    public Item(Long id, String sku, String name, String description, int availableQuantity) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.availableQuantity = availableQuantity;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getAvailableQuantity() { return availableQuantity; }
}

