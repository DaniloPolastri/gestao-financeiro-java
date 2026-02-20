package com.findash.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_imports", schema = "financial_schema")
public class BankImport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private BankImportFileType fileType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankImportStatus status = BankImportStatus.PENDING_REVIEW;

    @Column(name = "total_records", nullable = false)
    private int totalRecords;

    @Column(name = "imported_by")
    private UUID importedBy;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected BankImport() {}

    public BankImport(UUID companyId, String fileName, BankImportFileType fileType, UUID importedBy) {
        this.companyId = companyId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.importedBy = importedBy;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getFileName() { return fileName; }
    public BankImportFileType getFileType() { return fileType; }
    public BankImportStatus getStatus() { return status; }
    public int getTotalRecords() { return totalRecords; }
    public UUID getImportedBy() { return importedBy; }
    public String getBankName() { return bankName; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setBankName(String bankName) { this.bankName = bankName; }
    public void setStatus(BankImportStatus status) { this.status = status; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    public void setId(UUID id) { this.id = id; }
}
