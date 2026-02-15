package com.findash.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_roles", schema = "auth_schema",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "company_id"}))
public class UserRole extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
