package com.findash.repository;

import com.findash.entity.CompanyMember;
import com.findash.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, UUID> {
    List<CompanyMember> findByCompanyIdAndStatusNot(UUID companyId, MemberStatus status);
    Optional<CompanyMember> findByCompanyIdAndUserId(UUID companyId, UUID userId);
    boolean existsByCompanyIdAndInvitedEmailAndStatusIn(UUID companyId, String email, List<MemberStatus> statuses);
    List<CompanyMember> findByInvitedEmailAndStatus(String email, MemberStatus status);
}
