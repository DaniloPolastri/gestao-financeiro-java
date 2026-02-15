package com.findash.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public class UserRole {
    private UUID id;
    private UUID userId;
    private UUID companyId;
    private Role role;
    private Instant createdAt;

    public UserRole(UUID id, UUID userId, UUID companyId, Role role, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.companyId = companyId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCompanyId() { return companyId; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setRole(Role role) { this.role = role; }
}
