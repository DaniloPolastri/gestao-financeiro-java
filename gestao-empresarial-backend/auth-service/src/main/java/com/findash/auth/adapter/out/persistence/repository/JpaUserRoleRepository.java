package com.findash.auth.adapter.out.persistence.repository;

import com.findash.auth.adapter.out.persistence.entity.UserRoleEntity;
import com.findash.auth.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JpaUserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {
    List<UserRoleEntity> findByUserId(UUID userId);
    List<UserRoleEntity> findByCompanyId(UUID companyId);
    void deleteByUserIdAndCompanyId(UUID userId, UUID companyId);
    boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);
    long countByCompanyIdAndRole(UUID companyId, Role role);
}
