package com.findash.repository;

import com.findash.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    List<Supplier> findByCompanyIdAndActiveTrue(UUID companyId);

    List<Supplier> findByCompanyId(UUID companyId);

    Optional<Supplier> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);

    boolean existsByCompanyIdAndDocument(UUID companyId, String document);
}
