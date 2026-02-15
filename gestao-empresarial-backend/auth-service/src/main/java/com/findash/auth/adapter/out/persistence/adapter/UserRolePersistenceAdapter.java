package com.findash.auth.adapter.out.persistence.adapter;

import com.findash.auth.adapter.out.persistence.entity.UserRoleEntity;
import com.findash.auth.adapter.out.persistence.repository.JpaUserRoleRepository;
import com.findash.auth.domain.model.Role;
import com.findash.auth.domain.model.UserRole;
import com.findash.auth.domain.port.out.UserRoleRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class UserRolePersistenceAdapter implements UserRoleRepositoryPort {

    private final JpaUserRoleRepository jpaRepo;

    public UserRolePersistenceAdapter(JpaUserRoleRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public UserRole save(UserRole userRole) {
        UserRoleEntity entity = toEntity(userRole);
        UserRoleEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<UserRole> findByUserId(UUID userId) {
        return jpaRepo.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<UserRole> findByCompanyId(UUID companyId) {
        return jpaRepo.findByCompanyId(companyId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByUserIdAndCompanyId(UUID userId, UUID companyId) {
        jpaRepo.deleteByUserIdAndCompanyId(userId, companyId);
    }

    @Override
    public boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return jpaRepo.existsByUserIdAndCompanyId(userId, companyId);
    }

    @Override
    public long countByCompanyIdAndRole(UUID companyId, Role role) {
        return jpaRepo.countByCompanyIdAndRole(companyId, role);
    }

    private UserRoleEntity toEntity(UserRole ur) {
        UserRoleEntity entity = new UserRoleEntity();
        entity.setId(ur.getId());
        entity.setUserId(ur.getUserId());
        entity.setCompanyId(ur.getCompanyId());
        entity.setRole(ur.getRole());
        return entity;
    }

    private UserRole toDomain(UserRoleEntity entity) {
        return new UserRole(entity.getId(), entity.getUserId(), entity.getCompanyId(),
                            entity.getRole(), entity.getCreatedAt());
    }
}
