package com.findash.repository;

import com.findash.entity.Role;
import com.findash.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserId(UUID userId);
    List<UserRole> findByCompanyId(UUID companyId);
    void deleteByUserIdAndCompanyId(UUID userId, UUID companyId);
    boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);
    long countByCompanyIdAndRole(UUID companyId, Role role);
}
