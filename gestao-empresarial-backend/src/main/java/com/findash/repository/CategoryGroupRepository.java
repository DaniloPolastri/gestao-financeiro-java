package com.findash.repository;

import com.findash.entity.CategoryGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryGroupRepository extends JpaRepository<CategoryGroup, UUID> {
    List<CategoryGroup> findByCompanyIdOrderByDisplayOrderAsc(UUID companyId);
    Optional<CategoryGroup> findByIdAndCompanyId(UUID id, UUID companyId);
    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);
}
