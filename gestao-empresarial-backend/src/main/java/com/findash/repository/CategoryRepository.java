package com.findash.repository;

import com.findash.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByGroupIdAndActiveTrue(UUID groupId);
    List<Category> findByCompanyIdAndActiveTrue(UUID companyId);
    Optional<Category> findByIdAndCompanyId(UUID id, UUID companyId);
    boolean existsByGroupIdAndNameIgnoreCase(UUID groupId, String name);
    boolean existsByGroupIdAndActiveTrue(UUID groupId);
}
