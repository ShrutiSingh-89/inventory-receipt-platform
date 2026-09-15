package com.portfolio.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_organizations")
public class InventoryOrganization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;
    private String location;

    protected InventoryOrganization() {}

    public InventoryOrganization(Long id, String code, String name, String location) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.location = location;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getLocation() { return location; }
}
