package com.findash.controller;

import com.findash.dto.category.*;
import com.findash.security.CompanyContextHolder;
import com.findash.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // --- Groups ---

    @GetMapping("/groups")
    public ResponseEntity<List<CategoryGroupResponseDTO>> listGroups() {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(categoryService.listGroups(companyId));
    }

    @PostMapping("/groups")
    public ResponseEntity<CategoryGroupResponseDTO> createGroup(
            @Valid @RequestBody CreateCategoryGroupRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createGroup(companyId, request));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<CategoryGroupResponseDTO> updateGroup(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryGroupRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(categoryService.updateGroup(companyId, id, request));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        categoryService.deleteGroup(companyId, id);
        return ResponseEntity.noContent().build();
    }

    // --- Categories ---

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CreateCategoryRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(companyId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(categoryService.updateCategory(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        categoryService.deleteCategory(companyId, id);
        return ResponseEntity.noContent().build();
    }
}
