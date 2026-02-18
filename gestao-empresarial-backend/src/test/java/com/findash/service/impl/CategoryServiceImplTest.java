package com.findash.service.impl;

import com.findash.dto.category.*;
import com.findash.entity.Category;
import com.findash.entity.CategoryGroup;
import com.findash.entity.CategoryGroupType;
import com.findash.exception.BusinessRuleException;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.CategoryMapper;
import com.findash.repository.AccountRepository;
import com.findash.repository.CategoryGroupRepository;
import com.findash.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryGroupRepository groupRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CategoryMapper categoryMapper;

    private CategoryServiceImpl categoryService;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(groupRepository, categoryRepository, accountRepository, categoryMapper);
        companyId = UUID.randomUUID();
    }

    // --- GROUP CRUD ---

    @Test
    void listGroups_returnsGroupsWithCategories() {
        var group = new CategoryGroup(companyId, "Receita Operacional", CategoryGroupType.REVENUE, 1);
        group.setId(UUID.randomUUID());
        var cat = new Category(group.getId(), companyId, "Vendas");

        when(groupRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId)).thenReturn(List.of(group));
        when(categoryRepository.findByGroupIdAndActiveTrue(group.getId())).thenReturn(List.of(cat));
        when(categoryMapper.toGroupResponse(group, List.of(cat)))
            .thenReturn(new CategoryGroupResponseDTO(group.getId(), "Receita Operacional", "REVENUE", 1, List.of()));

        List<CategoryGroupResponseDTO> result = categoryService.listGroups(companyId);
        assertEquals(1, result.size());
    }

    @Test
    void createGroup_success() {
        var request = new CreateCategoryGroupRequestDTO("Novo Grupo", "EXPENSE");
        when(groupRepository.existsByCompanyIdAndNameIgnoreCase(companyId, "Novo Grupo")).thenReturn(false);
        when(groupRepository.save(any(CategoryGroup.class))).thenAnswer(inv -> {
            CategoryGroup g = inv.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });
        when(categoryMapper.toGroupResponse(any(), any()))
            .thenReturn(new CategoryGroupResponseDTO(UUID.randomUUID(), "Novo Grupo", "EXPENSE", 0, List.of()));

        CategoryGroupResponseDTO result = categoryService.createGroup(companyId, request);
        assertNotNull(result);
        assertEquals("Novo Grupo", result.name());
    }

    @Test
    void createGroup_duplicateName_throws() {
        var request = new CreateCategoryGroupRequestDTO("Existente", "REVENUE");
        when(groupRepository.existsByCompanyIdAndNameIgnoreCase(companyId, "Existente")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> categoryService.createGroup(companyId, request));
    }

    @Test
    void deleteGroup_withActiveCategories_throws() {
        UUID groupId = UUID.randomUUID();
        var group = new CategoryGroup(companyId, "G", CategoryGroupType.EXPENSE, 0);
        group.setId(groupId);

        when(groupRepository.findByIdAndCompanyId(groupId, companyId)).thenReturn(Optional.of(group));
        when(categoryRepository.existsByGroupIdAndActiveTrue(groupId)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> categoryService.deleteGroup(companyId, groupId));
    }

    @Test
    void deleteGroup_withNoActiveCategories_butAccountsReference_throws() {
        UUID groupId = UUID.randomUUID();
        var group = new CategoryGroup(companyId, "G", CategoryGroupType.EXPENSE, 0);
        group.setId(groupId);

        when(groupRepository.findByIdAndCompanyId(groupId, companyId)).thenReturn(Optional.of(group));
        when(categoryRepository.existsByGroupIdAndActiveTrue(groupId)).thenReturn(false);
        when(accountRepository.existsByCategoryGroupId(groupId)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> categoryService.deleteGroup(companyId, groupId));
    }

    // --- CATEGORY CRUD ---

    @Test
    void createCategory_success() {
        UUID groupId = UUID.randomUUID();
        var group = new CategoryGroup(companyId, "G", CategoryGroupType.EXPENSE, 0);
        group.setId(groupId);
        var request = new CreateCategoryRequestDTO(groupId, "Nova Categoria");

        when(groupRepository.findByIdAndCompanyId(groupId, companyId)).thenReturn(Optional.of(group));
        when(categoryRepository.existsByGroupIdAndNameIgnoreCaseAndActiveTrue(groupId, "Nova Categoria")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(categoryMapper.toCategoryResponse(any())).thenReturn(
            new CategoryResponseDTO(UUID.randomUUID(), groupId, "Nova Categoria", true));

        CategoryResponseDTO result = categoryService.createCategory(companyId, request);
        assertNotNull(result);
        assertEquals("Nova Categoria", result.name());
    }

    @Test
    void createCategory_duplicateActiveName_throws() {
        UUID groupId = UUID.randomUUID();
        var group = new CategoryGroup(companyId, "G", CategoryGroupType.EXPENSE, 0);
        group.setId(groupId);
        var request = new CreateCategoryRequestDTO(groupId, "Existente");

        when(groupRepository.findByIdAndCompanyId(groupId, companyId)).thenReturn(Optional.of(group));
        when(categoryRepository.existsByGroupIdAndNameIgnoreCaseAndActiveTrue(groupId, "Existente")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(companyId, request));
    }

    @Test
    void createCategory_sameNameAsSoftDeleted_succeeds() {
        UUID groupId = UUID.randomUUID();
        var group = new CategoryGroup(companyId, "G", CategoryGroupType.EXPENSE, 0);
        group.setId(groupId);
        var request = new CreateCategoryRequestDTO(groupId, "Deletada");

        when(groupRepository.findByIdAndCompanyId(groupId, companyId)).thenReturn(Optional.of(group));
        // soft-deleted one exists but active check returns false → should NOT throw
        when(categoryRepository.existsByGroupIdAndNameIgnoreCaseAndActiveTrue(groupId, "Deletada")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(categoryMapper.toCategoryResponse(any())).thenReturn(
            new CategoryResponseDTO(UUID.randomUUID(), groupId, "Deletada", true));

        assertDoesNotThrow(() -> categoryService.createCategory(companyId, request));
    }

    // --- DELETE CATEGORY ---

    @Test
    void deleteCategory_noLinkedAccounts_hardDeletes() {
        UUID groupId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        var category = new Category(groupId, companyId, "Vendas");
        category.setId(categoryId);

        when(categoryRepository.findByIdAndCompanyId(categoryId, companyId)).thenReturn(Optional.of(category));
        when(accountRepository.existsByCategoryId(categoryId)).thenReturn(false);

        categoryService.deleteCategory(companyId, categoryId);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_withLinkedAccounts_throws() {
        UUID groupId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        var category = new Category(groupId, companyId, "Vendas");
        category.setId(categoryId);

        when(categoryRepository.findByIdAndCompanyId(categoryId, companyId)).thenReturn(Optional.of(category));
        when(accountRepository.existsByCategoryId(categoryId)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> categoryService.deleteCategory(companyId, categoryId));
        verify(categoryRepository, never()).delete(any());
    }

    // --- SEED ---

    @Test
    void seedDefaultCategories_createsGroupsAndCategories() {
        when(groupRepository.save(any(CategoryGroup.class))).thenAnswer(inv -> {
            CategoryGroup g = inv.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });

        categoryService.seedDefaultCategories(companyId);

        verify(groupRepository, atLeast(6)).save(any(CategoryGroup.class));
        verify(categoryRepository, atLeast(15)).save(any(Category.class));
    }
}
