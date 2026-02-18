package com.findash.repository;

import com.findash.entity.SupplierMatchRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierMatchRuleRepository extends JpaRepository<SupplierMatchRule, UUID> {
    List<SupplierMatchRule> findByCompanyId(UUID companyId);
    Optional<SupplierMatchRule> findByCompanyIdAndPattern(UUID companyId, String pattern);
}
