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
    private String productCategory;
    private String description;
    private int availableQuantity;
    private boolean serialControlled;
    private String serialPrefix;
    private String searchAliases;

    protected Item() {}

    public Item(Long id, String sku, String name, String productCategory, String description, int availableQuantity) {
        this(id, sku, name, productCategory, description, availableQuantity, false, null, null);
    }

    public Item(Long id, String sku, String name, String productCategory, String description, int availableQuantity,
                boolean serialControlled, String serialPrefix, String searchAliases) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.productCategory = productCategory;
        this.description = description;
        this.availableQuantity = availableQuantity;
        this.serialControlled = serialControlled;
        this.serialPrefix = serialPrefix;
        this.searchAliases = searchAliases;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getProductCategory() { return productCategory; }
    public String getDescription() { return description; }
    public int getAvailableQuantity() { return availableQuantity; }
    public boolean isSerialControlled() { return serialControlled; }
    public String getSerialPrefix() { return serialPrefix; }
    public String getSearchAliases() { return searchAliases; }
}
