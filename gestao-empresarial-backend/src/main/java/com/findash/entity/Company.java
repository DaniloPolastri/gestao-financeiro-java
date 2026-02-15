package com.findash.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "companies", schema = "company_schema")
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String cnpj;

    private String segment;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private boolean active = true;

    public Company() {}

    public Company(String name, String cnpj, String segment, UUID ownerId) {
        this.name = name;
        this.cnpj = cnpj;
        this.segment = segment;
        this.ownerId = ownerId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
