package com.findash.auth.domain.port.in;

import com.findash.auth.domain.model.Role;
import com.findash.auth.domain.model.UserRole;
import java.util.List;
import java.util.UUID;

public interface ManageUserRolesUseCase {
    UserRole assignRole(UUID userId, UUID companyId, Role role);
    void updateRole(UUID userId, UUID companyId, Role newRole);
    void removeFromCompany(UUID userId, UUID companyId);
    List<UserRole> getUserRolesForCompany(UUID companyId);
}
