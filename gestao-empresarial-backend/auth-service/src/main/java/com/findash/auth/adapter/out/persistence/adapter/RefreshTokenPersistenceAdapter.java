package com.findash.auth.adapter.out.persistence.adapter;

import com.findash.auth.adapter.out.persistence.entity.RefreshTokenEntity;
import com.findash.auth.adapter.out.persistence.repository.JpaRefreshTokenRepository;
import com.findash.auth.domain.model.RefreshToken;
import com.findash.auth.domain.port.out.RefreshTokenRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final JpaRefreshTokenRepository jpaRepo;

    public RefreshTokenPersistenceAdapter(JpaRefreshTokenRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity entity = toEntity(token);
        RefreshTokenEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepo.findByToken(token).map(this::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId) {
        jpaRepo.revokeAllByUserId(userId);
    }

    private RefreshTokenEntity toEntity(RefreshToken token) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setToken(token.getToken());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setRevoked(token.isRevoked());
        return entity;
    }

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(entity.getId(), entity.getUserId(), entity.getToken(),
                                entity.getExpiresAt(), entity.isRevoked(), entity.getCreatedAt());
    }
}
