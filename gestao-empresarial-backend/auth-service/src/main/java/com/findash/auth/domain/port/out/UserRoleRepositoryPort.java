package com.findash.auth.domain.port.out;

import com.findash.auth.domain.model.Role;
import com.findash.auth.domain.model.UserRole;
import java.util.List;
import java.util.UUID;

public interface UserRoleRepositoryPort {
    UserRole save(UserRole userRole);
    List<UserRole> findByUserId(UUID userId);
    List<UserRole> findByCompanyId(UUID companyId);
    void deleteByUserIdAndCompanyId(UUID userId, UUID companyId);
    boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);
    long countByCompanyIdAndRole(UUID companyId, Role role);
}
