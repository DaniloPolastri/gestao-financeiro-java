package com.findash.service;

import com.findash.dto.*;

import java.util.List;
import java.util.UUID;

public interface CompanyService {
    CompanyResponseDTO createCompany(CreateCompanyRequestDTO request, UUID userId);
    List<CompanyResponseDTO> listUserCompanies(UUID userId);
    CompanyResponseDTO getCompany(UUID companyId, UUID userId);
    CompanyResponseDTO updateCompany(UUID companyId, UpdateCompanyRequestDTO request, UUID userId);
    List<CompanyMemberResponseDTO> listMembers(UUID companyId, UUID userId);
    CompanyMemberResponseDTO inviteMember(UUID companyId, InviteMemberRequestDTO request, UUID userId);
    void updateMemberRole(UUID companyId, UUID memberId, UpdateMemberRoleRequestDTO request, UUID userId);
    void removeMember(UUID companyId, UUID memberId, UUID userId);
}
