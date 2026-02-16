package com.findash.repository;

import com.findash.entity.Company;
import com.findash.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsByCnpj(String cnpj);

    @Query("SELECT c FROM Company c WHERE c.id IN " +
           "(SELECT cm.companyId FROM CompanyMember cm WHERE cm.userId = :userId AND cm.status = :status)")
    List<Company> findByMemberUserIdAndStatus(UUID userId, MemberStatus status);
}
