package com.findash.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "accounts", schema = "financial_schema")
public class Account extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.PENDING;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "supplier_id")
    private UUID supplierId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "recurrence_id")
    private UUID recurrenceId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_by")
    private UUID createdBy;

    protected Account() {}

    public Account(UUID companyId, AccountType type, String description, BigDecimal amount, LocalDate dueDate, UUID categoryId) {
        this.companyId = companyId;
        this.type = type;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
        this.categoryId = categoryId;
    }

    // Getters
    public UUID getCompanyId() { return companyId; }
    public AccountType getType() { return type; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public AccountStatus getStatus() { return status; }
    public UUID getCategoryId() { return categoryId; }
    public UUID getSupplierId() { return supplierId; }
    public UUID getClientId() { return clientId; }
    public UUID getRecurrenceId() { return recurrenceId; }
    public String getNotes() { return notes; }
    public boolean isActive() { return active; }
    public UUID getCreatedBy() { return createdBy; }

    // Setters
    public void setDescription(String description) { this.description = description; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    public void setRecurrenceId(UUID recurrenceId) { this.recurrenceId = recurrenceId; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
