package com.findash.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String token;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt;

    public RefreshToken(UUID id, UUID userId, String token, Instant expiresAt,
                        boolean revoked, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isValid() { return !revoked && !isExpired(); }
}
