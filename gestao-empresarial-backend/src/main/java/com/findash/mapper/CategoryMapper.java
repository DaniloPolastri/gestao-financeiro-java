package com.findash.mapper;

import com.findash.dto.category.CategoryGroupResponseDTO;
import com.findash.dto.category.CategoryResponseDTO;
import com.findash.entity.Category;
import com.findash.entity.CategoryGroup;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    default CategoryResponseDTO toCategoryResponse(Category category) {
        return new CategoryResponseDTO(
            category.getId(),
            category.getGroupId(),
            category.getName(),
            category.isActive()
        );
    }

    default CategoryGroupResponseDTO toGroupResponse(CategoryGroup group, List<Category> categories) {
        List<CategoryResponseDTO> categoryDTOs = categories.stream()
            .map(this::toCategoryResponse)
            .toList();
        return new CategoryGroupResponseDTO(
            group.getId(),
            group.getName(),
            group.getType().name(),
            group.getDisplayOrder(),
            categoryDTOs
        );
    }
}
