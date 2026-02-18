package com.findash.service;

import com.findash.dto.category.*;
import java.util.List;
import java.util.UUID;

public interface CategoryService {
    List<CategoryGroupResponseDTO> listGroups(UUID companyId);
    CategoryGroupResponseDTO createGroup(UUID companyId, CreateCategoryGroupRequestDTO request);
    CategoryGroupResponseDTO updateGroup(UUID companyId, UUID groupId, UpdateCategoryGroupRequestDTO request);
    void deleteGroup(UUID companyId, UUID groupId);

    CategoryResponseDTO createCategory(UUID companyId, CreateCategoryRequestDTO request);
    CategoryResponseDTO updateCategory(UUID companyId, UUID categoryId, UpdateCategoryRequestDTO request);
    void deleteCategory(UUID companyId, UUID categoryId);

    void seedDefaultCategories(UUID companyId);
}
