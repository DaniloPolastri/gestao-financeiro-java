package com.findash.repository;

import com.findash.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByCompanyIdAndActiveTrue(UUID companyId);

    List<Client> findByCompanyId(UUID companyId);

    Optional<Client> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndNameIgnoreCaseAndActiveTrue(UUID companyId, String name);

    boolean existsByCompanyIdAndDocumentAndActiveTrue(UUID companyId, String document);
}
