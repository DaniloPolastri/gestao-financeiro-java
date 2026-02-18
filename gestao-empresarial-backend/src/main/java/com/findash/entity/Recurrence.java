package com.findash.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recurrences", schema = "financial_schema")
public class Recurrence extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "max_occurrences")
    private Integer maxOccurrences;

    protected Recurrence() {}

    public Recurrence(UUID companyId, RecurrenceFrequency frequency, LocalDate startDate) {
        this.companyId = companyId;
        this.frequency = frequency;
        this.startDate = startDate;
    }

    // Getters and setters
    public UUID getCompanyId() { return companyId; }
    public RecurrenceFrequency getFrequency() { return frequency; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getMaxOccurrences() { return maxOccurrences; }
    public void setMaxOccurrences(Integer maxOccurrences) { this.maxOccurrences = maxOccurrences; }
}
