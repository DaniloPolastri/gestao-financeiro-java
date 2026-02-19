package com.findash.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "supplier_match_rules", schema = "financial_schema")
public class SupplierMatchRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String pattern;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected SupplierMatchRule() {}

    public SupplierMatchRule(UUID companyId, String pattern, UUID supplierId, UUID categoryId) {
        this.companyId = companyId;
        this.pattern = pattern;
        this.supplierId = supplierId;
        this.categoryId = categoryId;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getPattern() { return pattern; }
    public UUID getSupplierId() { return supplierId; }
    public UUID getCategoryId() { return categoryId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
}
