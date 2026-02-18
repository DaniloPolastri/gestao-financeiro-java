package com.findash.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "category_groups", schema = "financial_schema")
public class CategoryGroup extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private CategoryGroupType type;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public CategoryGroup() {}

    public CategoryGroup(UUID companyId, String name, CategoryGroupType type, int displayOrder) {
        this.companyId = companyId;
        this.name = name;
        this.type = type;
        this.displayOrder = displayOrder;
    }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CategoryGroupType getType() { return type; }
    public void setType(CategoryGroupType type) { this.type = type; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
