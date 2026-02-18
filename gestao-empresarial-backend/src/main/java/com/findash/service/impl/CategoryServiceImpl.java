package com.findash.service.impl;

import com.findash.dto.category.*;
import com.findash.entity.Category;
import com.findash.entity.CategoryGroup;
import com.findash.entity.CategoryGroupType;
import com.findash.exception.BusinessRuleException;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.CategoryMapper;
import com.findash.repository.CategoryGroupRepository;
import com.findash.repository.CategoryRepository;
import com.findash.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryGroupRepository groupRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryGroupRepository groupRepository,
                               CategoryRepository categoryRepository,
                               CategoryMapper categoryMapper) {
        this.groupRepository = groupRepository;
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryGroupResponseDTO> listGroups(UUID companyId) {
        return groupRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId)
                .stream()
                .map(group -> {
                    List<Category> categories = categoryRepository.findByGroupIdAndActiveTrue(group.getId());
                    return categoryMapper.toGroupResponse(group, categories);
                })
                .toList();
    }

    @Override
    public CategoryGroupResponseDTO createGroup(UUID companyId, CreateCategoryGroupRequestDTO request) {
        if (groupRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name())) {
            throw new DuplicateResourceException("Ja existe um grupo com este nome");
        }

        CategoryGroupType type = CategoryGroupType.valueOf(request.type());
        CategoryGroup group = new CategoryGroup(companyId, request.name().trim(), type, 0);
        group = groupRepository.save(group);

        List<Category> categories = categoryRepository.findByGroupIdAndActiveTrue(group.getId());
        return categoryMapper.toGroupResponse(group, categories);
    }

    @Override
    public CategoryGroupResponseDTO updateGroup(UUID companyId, UUID groupId, UpdateCategoryGroupRequestDTO request) {
        CategoryGroup group = groupRepository.findByIdAndCompanyId(groupId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de categoria", groupId));

        if (!group.getName().equalsIgnoreCase(request.name())
                && groupRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name())) {
            throw new DuplicateResourceException("Ja existe um grupo com este nome");
        }

        group.setName(request.name().trim());
        group = groupRepository.save(group);

        List<Category> categories = categoryRepository.findByGroupIdAndActiveTrue(group.getId());
        return categoryMapper.toGroupResponse(group, categories);
    }

    @Override
    public void deleteGroup(UUID companyId, UUID groupId) {
        CategoryGroup group = groupRepository.findByIdAndCompanyId(groupId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de categoria", groupId));

        if (categoryRepository.existsByGroupIdAndActiveTrue(groupId)) {
            throw new BusinessRuleException("Nao e possivel excluir grupo com categorias vinculadas. Remova ou reclassifique as categorias primeiro.");
        }

        groupRepository.delete(group);
    }

    @Override
    public CategoryResponseDTO createCategory(UUID companyId, CreateCategoryRequestDTO request) {
        groupRepository.findByIdAndCompanyId(request.groupId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de categoria", request.groupId()));

        if (categoryRepository.existsByGroupIdAndNameIgnoreCase(request.groupId(), request.name())) {
            throw new DuplicateResourceException("Ja existe uma categoria com este nome neste grupo");
        }

        Category category = new Category(request.groupId(), companyId, request.name().trim());
        category = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    public CategoryResponseDTO updateCategory(UUID companyId, UUID categoryId, UpdateCategoryRequestDTO request) {
        Category category = categoryRepository.findByIdAndCompanyId(categoryId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", categoryId));

        if (!category.getName().equalsIgnoreCase(request.name())
                && categoryRepository.existsByGroupIdAndNameIgnoreCase(category.getGroupId(), request.name())) {
            throw new DuplicateResourceException("Ja existe uma categoria com este nome neste grupo");
        }

        category.setName(request.name().trim());
        category = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    public void deleteCategory(UUID companyId, UUID categoryId) {
        Category category = categoryRepository.findByIdAndCompanyId(categoryId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", categoryId));
        category.setActive(false);
        categoryRepository.save(category);
    }

    @Override
    public void seedDefaultCategories(UUID companyId) {
        Map<String, SeedGroup> defaults = Map.of(
            "Receita Operacional", new SeedGroup(CategoryGroupType.REVENUE, 1,
                List.of("Vendas de Produtos", "Vendas de Servicos", "Outras Receitas")),
            "Receitas Financeiras", new SeedGroup(CategoryGroupType.REVENUE, 2,
                List.of("Rendimentos", "Juros Recebidos")),
            "Custo Operacional", new SeedGroup(CategoryGroupType.EXPENSE, 3,
                List.of("Materia-Prima", "Mao de Obra Direta", "Outros Custos")),
            "Despesas Administrativas", new SeedGroup(CategoryGroupType.EXPENSE, 4,
                List.of("Aluguel", "Salarios", "Material de Escritorio", "Seguros")),
            "Despesas Financeiras", new SeedGroup(CategoryGroupType.EXPENSE, 5,
                List.of("Juros Pagos", "Tarifas Bancarias", "IOF")),
            "Impostos e Tributos", new SeedGroup(CategoryGroupType.EXPENSE, 6,
                List.of("IRPJ", "CSLL", "PIS", "COFINS", "ISS", "ICMS"))
        );

        defaults.forEach((groupName, seed) -> {
            CategoryGroup group = new CategoryGroup(companyId, groupName, seed.type, seed.order);
            group = groupRepository.save(group);
            UUID groupId = group.getId();
            for (String catName : seed.categories) {
                categoryRepository.save(new Category(groupId, companyId, catName));
            }
        });
    }

    private record SeedGroup(CategoryGroupType type, int order, List<String> categories) {}
}
