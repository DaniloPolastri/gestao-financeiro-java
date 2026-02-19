package com.findash.repository;

import com.findash.entity.BankImport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankImportRepository extends JpaRepository<BankImport, UUID> {
    List<BankImport> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    Optional<BankImport> findByIdAndCompanyId(UUID id, UUID companyId);
}
