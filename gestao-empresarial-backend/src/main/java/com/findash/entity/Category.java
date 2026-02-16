package com.findash.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "categories", schema = "financial_schema")
public class Category extends BaseEntity {

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    public Category() {}

    public Category(UUID groupId, UUID companyId, String name) {
        this.groupId = groupId;
        this.companyId = companyId;
        this.name = name;
    }

    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
