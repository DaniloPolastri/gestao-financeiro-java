package com.findash.repository;

import com.findash.entity.BankImportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankImportItemRepository extends JpaRepository<BankImportItem, UUID> {
    List<BankImportItem> findByImportId(UUID importId);
    Optional<BankImportItem> findByIdAndImportId(UUID id, UUID importId);
}
