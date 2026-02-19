package com.findash.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "bank_import_items", schema = "financial_schema")
public class BankImportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "import_id", nullable = false)
    private UUID importId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankImportItemType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "supplier_id")
    private UUID supplierId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "possible_duplicate", nullable = false)
    private boolean possibleDuplicate = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_data", columnDefinition = "jsonb")
    private Map<String, Object> originalData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected BankImportItem() {}

    public BankImportItem(UUID importId, LocalDate date, String description,
                          BigDecimal amount, BankImportItemType type, AccountType accountType) {
        this.importId = importId;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.accountType = accountType;
    }

    public UUID getId() { return id; }
    public UUID getImportId() { return importId; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public BankImportItemType getType() { return type; }
    public AccountType getAccountType() { return accountType; }
    public UUID getSupplierId() { return supplierId; }
    public UUID getCategoryId() { return categoryId; }
    public boolean isPossibleDuplicate() { return possibleDuplicate; }
    public Map<String, Object> getOriginalData() { return originalData; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public void setPossibleDuplicate(boolean possibleDuplicate) { this.possibleDuplicate = possibleDuplicate; }
    public void setOriginalData(Map<String, Object> originalData) { this.originalData = originalData; }
    public void setId(UUID id) { this.id = id; }
}
