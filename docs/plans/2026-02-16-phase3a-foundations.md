# Phase 3a: Foundations (Suppliers, Clients, Categories) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the foundational entities (Suppliers, Clients, Categories & Groups) that are prerequisites for Accounts Payable/Receivable and Bank Import features.

**Architecture:** Three new domain entities in `financial_schema` — all scoped by `company_id` for multi-tenancy. Backend follows existing layered pattern (Entity → Repository → DTO → Mapper → Service → Controller). Frontend adds three new feature modules with list/form components using Signals + Reactive Forms. Default category groups are seeded on company creation.

**Tech Stack:** Spring Boot 4 (Java 21), Spring Data JPA, MapStruct, Flyway, PostgreSQL | Angular 21, Tailwind CSS 4, PrimeNG, Vitest

---

## Task 1: Database Migration — Financial Schema Tables

**Files:**
- Create: `gestao-empresarial-backend/src/main/resources/db/migration/V3__create_financial_tables.sql`
- Modify: `gestao-empresarial-backend/src/main/resources/application.yml` (add financial_schema to flyway)

**Step 1: Write the Flyway migration SQL**

```sql
-- V3__create_financial_tables.sql

CREATE SCHEMA IF NOT EXISTS financial_schema;

-- Suppliers (Fornecedores)
CREATE TABLE financial_schema.suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    document VARCHAR(14),
    email VARCHAR(255),
    phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_supplier_company FOREIGN KEY (company_id)
        REFERENCES company_schema.companies(id) ON DELETE CASCADE
);

CREATE INDEX idx_supplier_company ON financial_schema.suppliers(company_id);
CREATE INDEX idx_supplier_name ON financial_schema.suppliers(company_id, name);
CREATE UNIQUE INDEX idx_supplier_document ON financial_schema.suppliers(company_id, document)
    WHERE document IS NOT NULL;

-- Clients (Clientes)
CREATE TABLE financial_schema.clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    document VARCHAR(14),
    email VARCHAR(255),
    phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_client_company FOREIGN KEY (company_id)
        REFERENCES company_schema.companies(id) ON DELETE CASCADE
);

CREATE INDEX idx_client_company ON financial_schema.clients(company_id);
CREATE INDEX idx_client_name ON financial_schema.clients(company_id, name);
CREATE UNIQUE INDEX idx_client_document ON financial_schema.clients(company_id, document)
    WHERE document IS NOT NULL;

-- Category Groups (Grupos de Categoria)
CREATE TABLE financial_schema.category_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(7) NOT NULL CHECK (type IN ('REVENUE', 'EXPENSE')),
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_category_group_company FOREIGN KEY (company_id)
        REFERENCES company_schema.companies(id) ON DELETE CASCADE,
    CONSTRAINT uq_category_group_name UNIQUE (company_id, name)
);

CREATE INDEX idx_category_group_company ON financial_schema.category_groups(company_id);

-- Categories (Categorias)
CREATE TABLE financial_schema.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_category_group FOREIGN KEY (group_id)
        REFERENCES financial_schema.category_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_category_company FOREIGN KEY (company_id)
        REFERENCES company_schema.companies(id) ON DELETE CASCADE,
    CONSTRAINT uq_category_name_in_group UNIQUE (group_id, name)
);

CREATE INDEX idx_category_group ON financial_schema.categories(group_id);
CREATE INDEX idx_category_company ON financial_schema.categories(company_id);
```

**Step 2: Add financial_schema to Flyway config**

In `application.yml`, change flyway schemas line:

```yaml
# Before:
flyway:
  schemas: auth_schema,company_schema

# After:
flyway:
  schemas: auth_schema,company_schema,financial_schema
```

**Step 3: Run migration to verify**

Run: `cd gestao-empresarial-backend && ./mvnw flyway:migrate -Dflyway.configFiles=src/main/resources/application.yml`

Or start the app: `./mvnw spring-boot:run` (Flyway runs on startup)

Expected: Migration V3 applies successfully, 4 tables created in financial_schema.

**Step 4: Commit**

```bash
git add gestao-empresarial-backend/src/main/resources/db/migration/V3__create_financial_tables.sql
git add gestao-empresarial-backend/src/main/resources/application.yml
git commit -m "feat(db): add financial_schema with suppliers, clients, category_groups, categories tables"
```

---

## Task 2: Supplier Backend — Entity + Repository

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/entity/Supplier.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/repository/SupplierRepository.java`

**Step 1: Create Supplier entity**

Follow the same pattern as `Company.java` — extend `BaseEntity`, use `financial_schema`.

```java
package gestao.com.example.gestaoempresarialbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "suppliers", schema = "financial_schema")
public class Supplier extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column(length = 14)
    private String document;

    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    public Supplier() {}

    public Supplier(UUID companyId, String name) {
        this.companyId = companyId;
        this.name = name;
    }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

**Step 2: Create SupplierRepository**

```java
package gestao.com.example.gestaoempresarialbackend.repository;

import gestao.com.example.gestaoempresarialbackend.entity.Supplier;
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
```

**Step 3: Verify compilation**

Run: `cd gestao-empresarial-backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/entity/Supplier.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/repository/SupplierRepository.java
git commit -m "feat(supplier): add Supplier entity and repository"
```

---

## Task 3: Supplier Backend — DTOs + Mapper

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/supplier/CreateSupplierRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/supplier/UpdateSupplierRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/supplier/SupplierResponseDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/mapper/SupplierMapper.java`

**Step 1: Create DTOs**

```java
// CreateSupplierRequestDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.supplier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplierRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name,
    String document,
    String email,
    String phone
) {}
```

```java
// UpdateSupplierRequestDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.supplier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSupplierRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name,
    String document,
    String email,
    String phone
) {}
```

```java
// SupplierResponseDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.supplier;

import java.time.Instant;
import java.util.UUID;

public record SupplierResponseDTO(
    UUID id,
    String name,
    String document,
    String email,
    String phone,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
```

**Step 2: Create SupplierMapper**

```java
package gestao.com.example.gestaoempresarialbackend.mapper;

import gestao.com.example.gestaoempresarialbackend.dto.supplier.SupplierResponseDTO;
import gestao.com.example.gestaoempresarialbackend.entity.Supplier;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    default SupplierResponseDTO toResponse(Supplier supplier) {
        return new SupplierResponseDTO(
            supplier.getId(),
            supplier.getName(),
            supplier.getDocument(),
            supplier.getEmail(),
            supplier.getPhone(),
            supplier.isActive(),
            supplier.getCreatedAt(),
            supplier.getUpdatedAt()
        );
    }
}
```

**Step 3: Verify compilation**

Run: `cd gestao-empresarial-backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/supplier/
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/mapper/SupplierMapper.java
git commit -m "feat(supplier): add Supplier DTOs and mapper"
```

---

## Task 4: Supplier Backend — Service (TDD)

**Files:**
- Create: `gestao-empresarial-backend/src/test/java/gestao/com/example/gestaoempresarialbackend/service/SupplierServiceImplTest.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/SupplierService.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/impl/SupplierServiceImpl.java`

**Step 1: Write failing tests**

```java
package gestao.com.example.gestaoempresarialbackend.service;

import gestao.com.example.gestaoempresarialbackend.dto.supplier.CreateSupplierRequestDTO;
import gestao.com.example.gestaoempresarialbackend.dto.supplier.SupplierResponseDTO;
import gestao.com.example.gestaoempresarialbackend.dto.supplier.UpdateSupplierRequestDTO;
import gestao.com.example.gestaoempresarialbackend.entity.Supplier;
import gestao.com.example.gestaoempresarialbackend.exception.DuplicateResourceException;
import gestao.com.example.gestaoempresarialbackend.exception.ResourceNotFoundException;
import gestao.com.example.gestaoempresarialbackend.mapper.SupplierMapper;
import gestao.com.example.gestaoempresarialbackend.repository.SupplierRepository;
import gestao.com.example.gestaoempresarialbackend.service.impl.SupplierServiceImpl;
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
class SupplierServiceImplTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierMapper supplierMapper;

    private SupplierServiceImpl supplierService;

    private UUID companyId;

    @BeforeEach
    void setUp() {
        supplierService = new SupplierServiceImpl(supplierRepository, supplierMapper);
        companyId = UUID.randomUUID();
    }

    // --- CREATE ---

    @Test
    void createSupplier_success() {
        var request = new CreateSupplierRequestDTO("Fornecedor A", "11222333000181", "a@test.com", "11999990000");
        var supplier = new Supplier(companyId, "Fornecedor A");
        supplier.setId(UUID.randomUUID());
        var response = new SupplierResponseDTO(supplier.getId(), "Fornecedor A", "11222333000181", "a@test.com", "11999990000", true, null, null);

        when(supplierRepository.existsByCompanyIdAndNameIgnoreCase(companyId, "Fornecedor A")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);
        when(supplierMapper.toResponse(any(Supplier.class))).thenReturn(response);

        SupplierResponseDTO result = supplierService.create(companyId, request);

        assertNotNull(result);
        assertEquals("Fornecedor A", result.name());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createSupplier_duplicateName_throws() {
        var request = new CreateSupplierRequestDTO("Fornecedor A", null, null, null);
        when(supplierRepository.existsByCompanyIdAndNameIgnoreCase(companyId, "Fornecedor A")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> supplierService.create(companyId, request));
        verify(supplierRepository, never()).save(any());
    }

    // --- LIST ---

    @Test
    void listSuppliers_returnsAll() {
        var s1 = new Supplier(companyId, "A");
        var s2 = new Supplier(companyId, "B");
        when(supplierRepository.findByCompanyIdAndActiveTrue(companyId)).thenReturn(List.of(s1, s2));
        when(supplierMapper.toResponse(s1)).thenReturn(new SupplierResponseDTO(UUID.randomUUID(), "A", null, null, null, true, null, null));
        when(supplierMapper.toResponse(s2)).thenReturn(new SupplierResponseDTO(UUID.randomUUID(), "B", null, null, null, true, null, null));

        List<SupplierResponseDTO> result = supplierService.list(companyId);

        assertEquals(2, result.size());
    }

    // --- GET BY ID ---

    @Test
    void getSupplier_found() {
        UUID supplierId = UUID.randomUUID();
        var supplier = new Supplier(companyId, "A");
        supplier.setId(supplierId);
        var response = new SupplierResponseDTO(supplierId, "A", null, null, null, true, null, null);

        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId)).thenReturn(Optional.of(supplier));
        when(supplierMapper.toResponse(supplier)).thenReturn(response);

        SupplierResponseDTO result = supplierService.getById(companyId, supplierId);

        assertEquals("A", result.name());
    }

    @Test
    void getSupplier_notFound_throws() {
        UUID supplierId = UUID.randomUUID();
        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplierService.getById(companyId, supplierId));
    }

    // --- UPDATE ---

    @Test
    void updateSupplier_success() {
        UUID supplierId = UUID.randomUUID();
        var supplier = new Supplier(companyId, "Old Name");
        supplier.setId(supplierId);
        var request = new UpdateSupplierRequestDTO("New Name", null, null, null);
        var response = new SupplierResponseDTO(supplierId, "New Name", null, null, null, true, null, null);

        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId)).thenReturn(Optional.of(supplier));
        when(supplierRepository.existsByCompanyIdAndNameIgnoreCase(companyId, "New Name")).thenReturn(false);
        when(supplierRepository.save(supplier)).thenReturn(supplier);
        when(supplierMapper.toResponse(supplier)).thenReturn(response);

        SupplierResponseDTO result = supplierService.update(companyId, supplierId, request);

        assertEquals("New Name", result.name());
    }

    // --- DELETE (soft) ---

    @Test
    void deleteSupplier_setsInactive() {
        UUID supplierId = UUID.randomUUID();
        var supplier = new Supplier(companyId, "A");
        supplier.setId(supplierId);
        supplier.setActive(true);

        when(supplierRepository.findByIdAndCompanyId(supplierId, companyId)).thenReturn(Optional.of(supplier));

        supplierService.delete(companyId, supplierId);

        assertFalse(supplier.isActive());
        verify(supplierRepository).save(supplier);
    }
}
```

**Step 2: Run tests — verify they fail**

Run: `cd gestao-empresarial-backend && ./mvnw test -pl . -Dtest=SupplierServiceImplTest -q`

Expected: COMPILATION FAILURE (SupplierService and SupplierServiceImpl do not exist yet)

**Step 3: Create SupplierService interface**

```java
package gestao.com.example.gestaoempresarialbackend.service;

import gestao.com.example.gestaoempresarialbackend.dto.supplier.CreateSupplierRequestDTO;
import gestao.com.example.gestaoempresarialbackend.dto.supplier.SupplierResponseDTO;
import gestao.com.example.gestaoempresarialbackend.dto.supplier.UpdateSupplierRequestDTO;
import java.util.List;
import java.util.UUID;

public interface SupplierService {

    SupplierResponseDTO create(UUID companyId, CreateSupplierRequestDTO request);

    List<SupplierResponseDTO> list(UUID companyId);

    SupplierResponseDTO getById(UUID companyId, UUID supplierId);

    SupplierResponseDTO update(UUID companyId, UUID supplierId, UpdateSupplierRequestDTO request);

    void delete(UUID companyId, UUID supplierId);
}
```

**Step 4: Create SupplierServiceImpl**

```java
package gestao.com.example.gestaoempresarialbackend.service.impl;

import gestao.com.example.gestaoempresarialbackend.dto.supplier.CreateSupplierRequestDTO;
import gestao.com.example.gestaoempresarialbackend.dto.supplier.SupplierResponseDTO;
import gestao.com.example.gestaoempresarialbackend.dto.supplier.UpdateSupplierRequestDTO;
import gestao.com.example.gestaoempresarialbackend.entity.Supplier;
import gestao.com.example.gestaoempresarialbackend.exception.DuplicateResourceException;
import gestao.com.example.gestaoempresarialbackend.exception.ResourceNotFoundException;
import gestao.com.example.gestaoempresarialbackend.mapper.SupplierMapper;
import gestao.com.example.gestaoempresarialbackend.repository.SupplierRepository;
import gestao.com.example.gestaoempresarialbackend.service.SupplierService;
import gestao.com.example.gestaoempresarialbackend.util.CnpjValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    public SupplierServiceImpl(SupplierRepository supplierRepository, SupplierMapper supplierMapper) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
    }

    @Override
    public SupplierResponseDTO create(UUID companyId, CreateSupplierRequestDTO request) {
        if (supplierRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name())) {
            throw new DuplicateResourceException("Ja existe um fornecedor com este nome");
        }

        String normalizedDoc = request.document() != null ? CnpjValidator.normalize(request.document()) : null;
        if (normalizedDoc != null && !normalizedDoc.isBlank()
                && supplierRepository.existsByCompanyIdAndDocument(companyId, normalizedDoc)) {
            throw new DuplicateResourceException("Ja existe um fornecedor com este documento");
        }

        Supplier supplier = new Supplier(companyId, request.name().trim());
        supplier.setDocument(normalizedDoc != null && !normalizedDoc.isBlank() ? normalizedDoc : null);
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());

        supplier = supplierRepository.save(supplier);
        return supplierMapper.toResponse(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponseDTO> list(UUID companyId) {
        return supplierRepository.findByCompanyIdAndActiveTrue(companyId)
                .stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDTO getById(UUID companyId, UUID supplierId) {
        Supplier supplier = findOrThrow(companyId, supplierId);
        return supplierMapper.toResponse(supplier);
    }

    @Override
    public SupplierResponseDTO update(UUID companyId, UUID supplierId, UpdateSupplierRequestDTO request) {
        Supplier supplier = findOrThrow(companyId, supplierId);

        if (!supplier.getName().equalsIgnoreCase(request.name())
                && supplierRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name())) {
            throw new DuplicateResourceException("Ja existe um fornecedor com este nome");
        }

        supplier.setName(request.name().trim());
        String normalizedDoc = request.document() != null ? CnpjValidator.normalize(request.document()) : null;
        supplier.setDocument(normalizedDoc != null && !normalizedDoc.isBlank() ? normalizedDoc : null);
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());

        supplier = supplierRepository.save(supplier);
        return supplierMapper.toResponse(supplier);
    }

    @Override
    public void delete(UUID companyId, UUID supplierId) {
        Supplier supplier = findOrThrow(companyId, supplierId);
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    private Supplier findOrThrow(UUID companyId, UUID supplierId) {
        return supplierRepository.findByIdAndCompanyId(supplierId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor nao encontrado"));
    }
}
```

**Step 5: Run tests — verify they pass**

Run: `cd gestao-empresarial-backend && ./mvnw test -pl . -Dtest=SupplierServiceImplTest -q`

Expected: All 6 tests PASS

**Step 6: Commit**

```bash
git add gestao-empresarial-backend/src/test/java/gestao/com/example/gestaoempresarialbackend/service/SupplierServiceImplTest.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/SupplierService.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/impl/SupplierServiceImpl.java
git commit -m "feat(supplier): add SupplierService with TDD tests"
```

---

## Task 5: Supplier Backend — Controller

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/controller/SupplierController.java`

**Step 1: Create SupplierController**

Follow same pattern as `CompanyController`. Uses `CompanyContextHolder.get()` for company_id (set by `CompanyContextFilter` from `X-Company-Id` header).

```java
package gestao.com.example.gestaoempresarialbackend.controller;

import gestao.com.example.gestaoempresarialbackend.dto.supplier.CreateSupplierRequestDTO;
import gestao.com.example.gestaoempresarialbackend.dto.supplier.SupplierResponseDTO;
import gestao.com.example.gestaoempresarialbackend.dto.supplier.UpdateSupplierRequestDTO;
import gestao.com.example.gestaoempresarialbackend.security.CompanyContextHolder;
import gestao.com.example.gestaoempresarialbackend.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<SupplierResponseDTO> create(@Valid @RequestBody CreateSupplierRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(companyId, request));
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponseDTO>> list() {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(supplierService.list(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> getById(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(supplierService.getById(companyId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(supplierService.update(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        supplierService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }
}
```

**Step 2: Verify compilation**

Run: `cd gestao-empresarial-backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/controller/SupplierController.java
git commit -m "feat(supplier): add SupplierController REST endpoints"
```

---

## Task 6: Client Backend — Entity + Repository + DTOs + Mapper

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/entity/Client.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/repository/ClientRepository.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/client/CreateClientRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/client/UpdateClientRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/client/ClientResponseDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/mapper/ClientMapper.java`

**Step 1: Create Client entity** (identical structure to Supplier, table = `clients`)

```java
package gestao.com.example.gestaoempresarialbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "clients", schema = "financial_schema")
public class Client extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column(length = 14)
    private String document;

    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    public Client() {}

    public Client(UUID companyId, String name) {
        this.companyId = companyId;
        this.name = name;
    }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

**Step 2: Create ClientRepository**

```java
package gestao.com.example.gestaoempresarialbackend.repository;

import gestao.com.example.gestaoempresarialbackend.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByCompanyIdAndActiveTrue(UUID companyId);

    List<Client> findByCompanyId(UUID companyId);

    Optional<Client> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);

    boolean existsByCompanyIdAndDocument(UUID companyId, String document);
}
```

**Step 3: Create Client DTOs**

```java
// CreateClientRequestDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name,
    String document,
    String email,
    String phone
) {}
```

```java
// UpdateClientRequestDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateClientRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name,
    String document,
    String email,
    String phone
) {}
```

```java
// ClientResponseDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.client;

import java.time.Instant;
import java.util.UUID;

public record ClientResponseDTO(
    UUID id,
    String name,
    String document,
    String email,
    String phone,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
```

**Step 4: Create ClientMapper**

```java
package gestao.com.example.gestaoempresarialbackend.mapper;

import gestao.com.example.gestaoempresarialbackend.dto.client.ClientResponseDTO;
import gestao.com.example.gestaoempresarialbackend.entity.Client;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    default ClientResponseDTO toResponse(Client client) {
        return new ClientResponseDTO(
            client.getId(),
            client.getName(),
            client.getDocument(),
            client.getEmail(),
            client.getPhone(),
            client.isActive(),
            client.getCreatedAt(),
            client.getUpdatedAt()
        );
    }
}
```

**Step 5: Verify compilation**

Run: `cd gestao-empresarial-backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

**Step 6: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/entity/Client.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/repository/ClientRepository.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/client/
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/mapper/ClientMapper.java
git commit -m "feat(client): add Client entity, repository, DTOs, mapper"
```

---

## Task 7: Client Backend — Service (TDD) + Controller

**Files:**
- Create: `gestao-empresarial-backend/src/test/java/gestao/com/example/gestaoempresarialbackend/service/ClientServiceImplTest.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/ClientService.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/impl/ClientServiceImpl.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/controller/ClientController.java`

**Step 1: Write failing tests**

Same pattern as SupplierServiceImplTest — replace "Supplier" with "Client", "fornecedor" with "cliente".

```java
package gestao.com.example.gestaoempresarialbackend.service;

import gestao.com.example.gestaoempresarialbackend.dto.client.CreateClientRequestDTO;
import gestao.com.example.gestaoempresarialbackend.dto.client.ClientResponseDTO;
import gestao.com.example.gestaoempresarialbackend.dto.client.UpdateClientRequestDTO;
import gestao.com.example.gestaoempresarialbackend.entity.Client;
import gestao.com.example.gestaoempresarialbackend.exception.DuplicateResourceException;
import gestao.com.example.gestaoempresarialbackend.exception.ResourceNotFoundException;
import gestao.com.example.gestaoempresarialbackend.mapper.ClientMapper;
import gestao.com.example.gestaoempresarialbackend.repository.ClientRepository;
import gestao.com.example.gestaoempresarialbackend.service.impl.ClientServiceImpl;
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
class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private ClientMapper clientMapper;

    private ClientServiceImpl clientService;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        clientService = new ClientServiceImpl(clientRepository, clientMapper);
        companyId = UUID.randomUUID();
    }

    @Test
    void createClient_success() {
        var request = new CreateClientRequestDTO("Cliente A", "11222333000181", "a@test.com", "11999990000");
        var client = new Client(companyId, "Cliente A");
        client.setId(UUID.randomUUID());
        var response = new ClientResponseDTO(client.getId(), "Cliente A", "11222333000181", "a@test.com", "11999990000", true, null, null);

        when(clientRepository.existsByCompanyIdAndNameIgnoreCase(companyId, "Cliente A")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(clientMapper.toResponse(any(Client.class))).thenReturn(response);

        ClientResponseDTO result = clientService.create(companyId, request);
        assertNotNull(result);
        assertEquals("Cliente A", result.name());
    }

    @Test
    void createClient_duplicateName_throws() {
        var request = new CreateClientRequestDTO("Cliente A", null, null, null);
        when(clientRepository.existsByCompanyIdAndNameIgnoreCase(companyId, "Cliente A")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> clientService.create(companyId, request));
    }

    @Test
    void listClients_returnsAll() {
        var c1 = new Client(companyId, "A");
        when(clientRepository.findByCompanyIdAndActiveTrue(companyId)).thenReturn(List.of(c1));
        when(clientMapper.toResponse(c1)).thenReturn(new ClientResponseDTO(UUID.randomUUID(), "A", null, null, null, true, null, null));

        List<ClientResponseDTO> result = clientService.list(companyId);
        assertEquals(1, result.size());
    }

    @Test
    void getClient_notFound_throws() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findByIdAndCompanyId(clientId, companyId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> clientService.getById(companyId, clientId));
    }

    @Test
    void deleteClient_setsInactive() {
        UUID clientId = UUID.randomUUID();
        var client = new Client(companyId, "A");
        client.setId(clientId);
        when(clientRepository.findByIdAndCompanyId(clientId, companyId)).thenReturn(Optional.of(client));

        clientService.delete(companyId, clientId);
        assertFalse(client.isActive());
        verify(clientRepository).save(client);
    }
}
```

**Step 2: Create ClientService interface**

```java
package gestao.com.example.gestaoempresarialbackend.service;

import gestao.com.example.gestaoempresarialbackend.dto.client.CreateClientRequestDTO;
import gestao.com.example.gestaoempresarialbackend.dto.client.ClientResponseDTO;
import gestao.com.example.gestaoempresarialbackend.dto.client.UpdateClientRequestDTO;
import java.util.List;
import java.util.UUID;

public interface ClientService {
    ClientResponseDTO create(UUID companyId, CreateClientRequestDTO request);
    List<ClientResponseDTO> list(UUID companyId);
    ClientResponseDTO getById(UUID companyId, UUID clientId);
    ClientResponseDTO update(UUID companyId, UUID clientId, UpdateClientRequestDTO request);
    void delete(UUID companyId, UUID clientId);
}
```

**Step 3: Create ClientServiceImpl**

Same logic as SupplierServiceImpl — replace Supplier→Client, fornecedor→cliente.

```java
package gestao.com.example.gestaoempresarialbackend.service.impl;

import gestao.com.example.gestaoempresarialbackend.dto.client.CreateClientRequestDTO;
import gestao.com.example.gestaoempresarialbackend.dto.client.ClientResponseDTO;
import gestao.com.example.gestaoempresarialbackend.dto.client.UpdateClientRequestDTO;
import gestao.com.example.gestaoempresarialbackend.entity.Client;
import gestao.com.example.gestaoempresarialbackend.exception.DuplicateResourceException;
import gestao.com.example.gestaoempresarialbackend.exception.ResourceNotFoundException;
import gestao.com.example.gestaoempresarialbackend.mapper.ClientMapper;
import gestao.com.example.gestaoempresarialbackend.repository.ClientRepository;
import gestao.com.example.gestaoempresarialbackend.service.ClientService;
import gestao.com.example.gestaoempresarialbackend.util.CnpjValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public ClientServiceImpl(ClientRepository clientRepository, ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
    }

    @Override
    public ClientResponseDTO create(UUID companyId, CreateClientRequestDTO request) {
        if (clientRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name())) {
            throw new DuplicateResourceException("Ja existe um cliente com este nome");
        }

        String normalizedDoc = request.document() != null ? CnpjValidator.normalize(request.document()) : null;
        if (normalizedDoc != null && !normalizedDoc.isBlank()
                && clientRepository.existsByCompanyIdAndDocument(companyId, normalizedDoc)) {
            throw new DuplicateResourceException("Ja existe um cliente com este documento");
        }

        Client client = new Client(companyId, request.name().trim());
        client.setDocument(normalizedDoc != null && !normalizedDoc.isBlank() ? normalizedDoc : null);
        client.setEmail(request.email());
        client.setPhone(request.phone());

        client = clientRepository.save(client);
        return clientMapper.toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponseDTO> list(UUID companyId) {
        return clientRepository.findByCompanyIdAndActiveTrue(companyId)
                .stream()
                .map(clientMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDTO getById(UUID companyId, UUID clientId) {
        Client client = findOrThrow(companyId, clientId);
        return clientMapper.toResponse(client);
    }

    @Override
    public ClientResponseDTO update(UUID companyId, UUID clientId, UpdateClientRequestDTO request) {
        Client client = findOrThrow(companyId, clientId);

        if (!client.getName().equalsIgnoreCase(request.name())
                && clientRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name())) {
            throw new DuplicateResourceException("Ja existe um cliente com este nome");
        }

        client.setName(request.name().trim());
        String normalizedDoc = request.document() != null ? CnpjValidator.normalize(request.document()) : null;
        client.setDocument(normalizedDoc != null && !normalizedDoc.isBlank() ? normalizedDoc : null);
        client.setEmail(request.email());
        client.setPhone(request.phone());

        client = clientRepository.save(client);
        return clientMapper.toResponse(client);
    }

    @Override
    public void delete(UUID companyId, UUID clientId) {
        Client client = findOrThrow(companyId, clientId);
        client.setActive(false);
        clientRepository.save(client);
    }

    private Client findOrThrow(UUID companyId, UUID clientId) {
        return clientRepository.findByIdAndCompanyId(clientId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado"));
    }
}
```

**Step 4: Create ClientController**

```java
package gestao.com.example.gestaoempresarialbackend.controller;

import gestao.com.example.gestaoempresarialbackend.dto.client.CreateClientRequestDTO;
import gestao.com.example.gestaoempresarialbackend.dto.client.ClientResponseDTO;
import gestao.com.example.gestaoempresarialbackend.dto.client.UpdateClientRequestDTO;
import gestao.com.example.gestaoempresarialbackend.security.CompanyContextHolder;
import gestao.com.example.gestaoempresarialbackend.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientResponseDTO> create(@Valid @RequestBody CreateClientRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(companyId, request));
    }

    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> list() {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(clientService.list(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getById(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(clientService.getById(companyId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClientRequestDTO request) {
        UUID companyId = CompanyContextHolder.get();
        return ResponseEntity.ok(clientService.update(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        UUID companyId = CompanyContextHolder.get();
        clientService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }
}
```

**Step 5: Run all tests**

Run: `cd gestao-empresarial-backend && ./mvnw test -q`

Expected: All tests PASS

**Step 6: Commit**

```bash
git add gestao-empresarial-backend/src/test/java/gestao/com/example/gestaoempresarialbackend/service/ClientServiceImplTest.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/ClientService.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/impl/ClientServiceImpl.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/controller/ClientController.java
git commit -m "feat(client): add Client service (TDD), controller, full CRUD"
```

---

## Task 8: Category Backend — Entities + Repositories

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/entity/CategoryGroupType.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/entity/CategoryGroup.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/entity/Category.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/repository/CategoryGroupRepository.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/repository/CategoryRepository.java`

**Step 1: Create CategoryGroupType enum**

```java
package gestao.com.example.gestaoempresarialbackend.entity;

public enum CategoryGroupType {
    REVENUE,
    EXPENSE
}
```

**Step 2: Create CategoryGroup entity**

```java
package gestao.com.example.gestaoempresarialbackend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "category_groups", schema = "financial_schema")
public class CategoryGroup extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private CategoryGroupType type;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public CategoryGroup() {}

    public CategoryGroup(UUID companyId, String name, CategoryGroupType type, int displayOrder) {
        this.companyId = companyId;
        this.name = name;
        this.type = type;
        this.displayOrder = displayOrder;
    }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CategoryGroupType getType() { return type; }
    public void setType(CategoryGroupType type) { this.type = type; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
```

**Step 3: Create Category entity**

```java
package gestao.com.example.gestaoempresarialbackend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "categories", schema = "financial_schema")
public class Category extends BaseEntity {

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    public Category() {}

    public Category(UUID groupId, UUID companyId, String name) {
        this.groupId = groupId;
        this.companyId = companyId;
        this.name = name;
    }

    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

**Step 4: Create repositories**

```java
// CategoryGroupRepository.java
package gestao.com.example.gestaoempresarialbackend.repository;

import gestao.com.example.gestaoempresarialbackend.entity.CategoryGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryGroupRepository extends JpaRepository<CategoryGroup, UUID> {
    List<CategoryGroup> findByCompanyIdOrderByDisplayOrderAsc(UUID companyId);
    Optional<CategoryGroup> findByIdAndCompanyId(UUID id, UUID companyId);
    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);
}
```

```java
// CategoryRepository.java
package gestao.com.example.gestaoempresarialbackend.repository;

import gestao.com.example.gestaoempresarialbackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByGroupIdAndActiveTrue(UUID groupId);
    List<Category> findByCompanyIdAndActiveTrue(UUID companyId);
    Optional<Category> findByIdAndCompanyId(UUID id, UUID companyId);
    boolean existsByGroupIdAndNameIgnoreCase(UUID groupId, String name);
    boolean existsByGroupId(UUID groupId);
}
```

**Step 5: Verify compilation**

Run: `cd gestao-empresarial-backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

**Step 6: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/entity/CategoryGroupType.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/entity/CategoryGroup.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/entity/Category.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/repository/CategoryGroupRepository.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/repository/CategoryRepository.java
git commit -m "feat(category): add CategoryGroup, Category entities and repositories"
```

---

## Task 9: Category Backend — DTOs + Mapper

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/category/CreateCategoryGroupRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/category/UpdateCategoryGroupRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/category/CategoryGroupResponseDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/category/CreateCategoryRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/category/UpdateCategoryRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/category/CategoryResponseDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/mapper/CategoryMapper.java`

**Step 1: Create DTOs**

```java
// CreateCategoryGroupRequestDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryGroupRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name,
    @NotNull(message = "Tipo e obrigatorio (REVENUE ou EXPENSE)")
    String type
) {}
```

```java
// UpdateCategoryGroupRequestDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryGroupRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name
) {}
```

```java
// CategoryGroupResponseDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.category;

import java.util.List;
import java.util.UUID;

public record CategoryGroupResponseDTO(
    UUID id,
    String name,
    String type,
    int displayOrder,
    List<CategoryResponseDTO> categories
) {}
```

```java
// CreateCategoryRequestDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateCategoryRequestDTO(
    @NotNull(message = "Grupo e obrigatorio")
    UUID groupId,
    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name
) {}
```

```java
// UpdateCategoryRequestDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name
) {}
```

```java
// CategoryResponseDTO.java
package gestao.com.example.gestaoempresarialbackend.dto.category;

import java.util.UUID;

public record CategoryResponseDTO(
    UUID id,
    UUID groupId,
    String name,
    boolean active
) {}
```

**Step 2: Create CategoryMapper**

```java
package gestao.com.example.gestaoempresarialbackend.mapper;

import gestao.com.example.gestaoempresarialbackend.dto.category.CategoryGroupResponseDTO;
import gestao.com.example.gestaoempresarialbackend.dto.category.CategoryResponseDTO;
import gestao.com.example.gestaoempresarialbackend.entity.Category;
import gestao.com.example.gestaoempresarialbackend.entity.CategoryGroup;
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
```

**Step 3: Verify compilation**

Run: `cd gestao-empresarial-backend && ./mvnw compile -q`

Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/dto/category/
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/mapper/CategoryMapper.java
git commit -m "feat(category): add Category DTOs and mapper"
```

---

## Task 10: Category Backend — Service (TDD)

**Files:**
- Create: `gestao-empresarial-backend/src/test/java/gestao/com/example/gestaoempresarialbackend/service/CategoryServiceImplTest.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/CategoryService.java`
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/impl/CategoryServiceImpl.java`

**Step 1: Write failing tests**

```java
package gestao.com.example.gestaoempresarialbackend.service;

import gestao.com.example.gestaoempresarialbackend.dto.category.*;
import gestao.com.example.gestaoempresarialbackend.entity.Category;
import gestao.com.example.gestaoempresarialbackend.entity.CategoryGroup;
import gestao.com.example.gestaoempresarialbackend.entity.CategoryGroupType;
import gestao.com.example.gestaoempresarialbackend.exception.BusinessRuleException;
import gestao.com.example.gestaoempresarialbackend.exception.DuplicateResourceException;
import gestao.com.example.gestaoempresarialbackend.exception.ResourceNotFoundException;
import gestao.com.example.gestaoempresarialbackend.mapper.CategoryMapper;
import gestao.com.example.gestaoempresarialbackend.repository.CategoryGroupRepository;
import gestao.com.example.gestaoempresarialbackend.repository.CategoryRepository;
import gestao.com.example.gestaoempresarialbackend.service.impl.CategoryServiceImpl;
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
    @Mock private CategoryMapper categoryMapper;

    private CategoryServiceImpl categoryService;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(groupRepository, categoryRepository, categoryMapper);
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
    void deleteGroup_withCategories_throws() {
        UUID groupId = UUID.randomUUID();
        var group = new CategoryGroup(companyId, "G", CategoryGroupType.EXPENSE, 0);
        group.setId(groupId);

        when(groupRepository.findByIdAndCompanyId(groupId, companyId)).thenReturn(Optional.of(group));
        when(categoryRepository.existsByGroupId(groupId)).thenReturn(true);

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
        when(categoryRepository.existsByGroupIdAndNameIgnoreCase(groupId, "Nova Categoria")).thenReturn(false);
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
    void createCategory_duplicateInGroup_throws() {
        UUID groupId = UUID.randomUUID();
        var group = new CategoryGroup(companyId, "G", CategoryGroupType.EXPENSE, 0);
        group.setId(groupId);
        var request = new CreateCategoryRequestDTO(groupId, "Existente");

        when(groupRepository.findByIdAndCompanyId(groupId, companyId)).thenReturn(Optional.of(group));
        when(categoryRepository.existsByGroupIdAndNameIgnoreCase(groupId, "Existente")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(companyId, request));
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
```

**Step 2: Create CategoryService interface**

```java
package gestao.com.example.gestaoempresarialbackend.service;

import gestao.com.example.gestaoempresarialbackend.dto.category.*;
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
```

**Step 3: Create CategoryServiceImpl**

```java
package gestao.com.example.gestaoempresarialbackend.service.impl;

import gestao.com.example.gestaoempresarialbackend.dto.category.*;
import gestao.com.example.gestaoempresarialbackend.entity.Category;
import gestao.com.example.gestaoempresarialbackend.entity.CategoryGroup;
import gestao.com.example.gestaoempresarialbackend.entity.CategoryGroupType;
import gestao.com.example.gestaoempresarialbackend.exception.BusinessRuleException;
import gestao.com.example.gestaoempresarialbackend.exception.DuplicateResourceException;
import gestao.com.example.gestaoempresarialbackend.exception.ResourceNotFoundException;
import gestao.com.example.gestaoempresarialbackend.mapper.CategoryMapper;
import gestao.com.example.gestaoempresarialbackend.repository.CategoryGroupRepository;
import gestao.com.example.gestaoempresarialbackend.repository.CategoryRepository;
import gestao.com.example.gestaoempresarialbackend.service.CategoryService;
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
                .orElseThrow(() -> new ResourceNotFoundException("Grupo nao encontrado"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Grupo nao encontrado"));

        if (categoryRepository.existsByGroupId(groupId)) {
            throw new BusinessRuleException("Nao e possivel excluir grupo com categorias vinculadas. Remova ou reclassifique as categorias primeiro.");
        }

        groupRepository.delete(group);
    }

    @Override
    public CategoryResponseDTO createCategory(UUID companyId, CreateCategoryRequestDTO request) {
        groupRepository.findByIdAndCompanyId(request.groupId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo nao encontrado"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Categoria nao encontrada"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Categoria nao encontrada"));
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
```

**Step 4: Run tests**

Run: `cd gestao-empresarial-backend && ./mvnw test -pl . -Dtest=CategoryServiceImplTest -q`

Expected: All 7 tests PASS

**Step 5: Commit**

```bash
git add gestao-empresarial-backend/src/test/java/gestao/com/example/gestaoempresarialbackend/service/CategoryServiceImplTest.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/CategoryService.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/impl/CategoryServiceImpl.java
git commit -m "feat(category): add CategoryService with TDD tests and default seeding"
```

---

## Task 11: Category Backend — Controller + Seed on Company Creation

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/controller/CategoryController.java`
- Modify: `gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/impl/CompanyServiceImpl.java` (inject CategoryService, call seedDefaultCategories on createCompany)

**Step 1: Create CategoryController**

```java
package gestao.com.example.gestaoempresarialbackend.controller;

import gestao.com.example.gestaoempresarialbackend.dto.category.*;
import gestao.com.example.gestaoempresarialbackend.security.CompanyContextHolder;
import gestao.com.example.gestaoempresarialbackend.service.CategoryService;
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
```

**Step 2: Modify CompanyServiceImpl to seed categories on company creation**

In `CompanyServiceImpl.java`, add `CategoryService` as a constructor dependency and call `categoryService.seedDefaultCategories(company.getId())` at the end of `createCompany()` method, right before the return statement.

Add to constructor parameters:
```java
private final CategoryService categoryService;

public CompanyServiceImpl(CompanyRepository companyRepository,
                          CompanyMemberRepository companyMemberRepository,
                          UserRoleRepository userRoleRepository,
                          UserRepository userRepository,
                          CompanyMapper companyMapper,
                          CategoryService categoryService) {
    // ... existing assignments ...
    this.categoryService = categoryService;
}
```

Add at end of `createCompany()`, before return:
```java
categoryService.seedDefaultCategories(company.getId());
```

**Step 3: Update CompanyServiceImplTest**

Add `@Mock CategoryService categoryService;` and pass it to the constructor in `setUp()`.

**Step 4: Run all backend tests**

Run: `cd gestao-empresarial-backend && ./mvnw test -q`

Expected: All tests PASS

**Step 5: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/controller/CategoryController.java
git add gestao-empresarial-backend/src/main/java/gestao/com/example/gestaoempresarialbackend/service/impl/CompanyServiceImpl.java
git add gestao-empresarial-backend/src/test/java/gestao/com/example/gestaoempresarialbackend/service/CompanyServiceImplTest.java
git commit -m "feat(category): add CategoryController, seed defaults on company creation"
```

---

## Task 12: Frontend — Supplier Feature (Model + Service + Components + Routes)

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/suppliers/models/supplier.model.ts`
- Create: `gestao-empresaial-frontend/src/app/features/suppliers/services/supplier.service.ts`
- Create: `gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-list/supplier-list.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-list/supplier-list.component.html`
- Create: `gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-form/supplier-form.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-form/supplier-form.component.html`
- Create: `gestao-empresaial-frontend/src/app/features/suppliers/suppliers.routes.ts`

**Step 1: Create model**

```typescript
// supplier.model.ts
export interface SupplierResponse {
  id: string;
  name: string;
  document: string | null;
  email: string | null;
  phone: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSupplierRequest {
  name: string;
  document?: string;
  email?: string;
  phone?: string;
}

export interface UpdateSupplierRequest {
  name: string;
  document?: string;
  email?: string;
  phone?: string;
}
```

**Step 2: Create service**

```typescript
// supplier.service.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { SupplierResponse, CreateSupplierRequest, UpdateSupplierRequest } from '../models/supplier.model';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/suppliers';

  private readonly _suppliers = signal<SupplierResponse[]>([]);
  readonly suppliers = this._suppliers.asReadonly();
  readonly hasSuppliers = computed(() => this._suppliers().length > 0);

  loadSuppliers(): Observable<SupplierResponse[]> {
    return this.http.get<SupplierResponse[]>(this.API_URL).pipe(
      tap((suppliers) => this._suppliers.set(suppliers)),
    );
  }

  getById(id: string): Observable<SupplierResponse> {
    return this.http.get<SupplierResponse>(`${this.API_URL}/${id}`);
  }

  create(data: CreateSupplierRequest): Observable<SupplierResponse> {
    return this.http.post<SupplierResponse>(this.API_URL, data);
  }

  update(id: string, data: UpdateSupplierRequest): Observable<SupplierResponse> {
    return this.http.put<SupplierResponse>(`${this.API_URL}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
```

**Step 3: Create SupplierListComponent**

```typescript
// supplier-list.component.ts
import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SupplierService } from '../../services/supplier.service';
import { SupplierResponse } from '../../models/supplier.model';

@Component({
  selector: 'app-supplier-list',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './supplier-list.component.html',
})
export class SupplierListComponent implements OnInit {
  private readonly supplierService = inject(SupplierService);

  protected readonly suppliers = this.supplierService.suppliers;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit() {
    this.supplierService.loadSuppliers().subscribe({
      next: () => this.loading.set(false),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao carregar fornecedores');
      },
    });
  }

  protected deleteSupplier(supplier: SupplierResponse) {
    if (!confirm(`Deseja realmente excluir o fornecedor "${supplier.name}"?`)) return;

    this.supplierService.delete(supplier.id).subscribe({
      next: () => this.supplierService.loadSuppliers().subscribe(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir fornecedor'),
    });
  }
}
```

```html
<!-- supplier-list.component.html -->
<div class="p-6">
  <div class="flex items-center justify-between mb-6">
    <h1 class="text-2xl font-bold text-gray-900">Fornecedores</h1>
    <a routerLink="novo"
       class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700">
      Novo Fornecedor
    </a>
  </div>

  @if (error()) {
    <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
      {{ error() }}
    </div>
  }

  @if (loading()) {
    <div class="text-sm text-gray-500">Carregando...</div>
  } @else if (suppliers().length === 0) {
    <div class="text-center py-12">
      <p class="text-gray-500 text-sm">Nenhum fornecedor cadastrado.</p>
      <a routerLink="novo" class="text-blue-600 text-sm hover:underline mt-2 inline-block">
        Cadastrar primeiro fornecedor
      </a>
    </div>
  } @else {
    <div class="border border-gray-200 rounded-lg overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 border-b border-gray-200">
          <tr>
            <th class="text-left px-4 py-3 font-medium text-gray-700">Nome</th>
            <th class="text-left px-4 py-3 font-medium text-gray-700">CNPJ/CPF</th>
            <th class="text-left px-4 py-3 font-medium text-gray-700">Email</th>
            <th class="text-left px-4 py-3 font-medium text-gray-700">Telefone</th>
            <th class="text-right px-4 py-3 font-medium text-gray-700">Acoes</th>
          </tr>
        </thead>
        <tbody>
          @for (supplier of suppliers(); track supplier.id) {
            <tr class="border-b border-gray-100 hover:bg-gray-50">
              <td class="px-4 py-3 text-gray-900">{{ supplier.name }}</td>
              <td class="px-4 py-3 text-gray-600">{{ supplier.document || '—' }}</td>
              <td class="px-4 py-3 text-gray-600">{{ supplier.email || '—' }}</td>
              <td class="px-4 py-3 text-gray-600">{{ supplier.phone || '—' }}</td>
              <td class="px-4 py-3 text-right">
                <a [routerLink]="[supplier.id, 'editar']"
                   class="text-blue-600 hover:underline mr-3">Editar</a>
                <button (click)="deleteSupplier(supplier)"
                        class="text-red-600 hover:underline">Excluir</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  }
</div>
```

**Step 4: Create SupplierFormComponent**

```typescript
// supplier-form.component.ts
import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { SupplierService } from '../../services/supplier.service';

@Component({
  selector: 'app-supplier-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './supplier-form.component.html',
})
export class SupplierFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly supplierService = inject(SupplierService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);
  private supplierId: string | null = null;

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    document: [''],
    email: [''],
    phone: [''],
  });

  ngOnInit() {
    this.supplierId = this.route.snapshot.paramMap.get('id');
    if (this.supplierId) {
      this.isEdit.set(true);
      this.loading.set(true);
      this.supplierService.getById(this.supplierId).subscribe({
        next: (supplier) => {
          this.form.patchValue({
            name: supplier.name,
            document: supplier.document || '',
            email: supplier.email || '',
            phone: supplier.phone || '',
          });
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Erro ao carregar fornecedor');
          this.loading.set(false);
        },
      });
    }
  }

  protected save() {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { name, document, email, phone } = this.form.getRawValue();
    const data = {
      name,
      document: document || undefined,
      email: email || undefined,
      phone: phone || undefined,
    };

    const request$ = this.isEdit()
      ? this.supplierService.update(this.supplierId!, data)
      : this.supplierService.create(data);

    request$.subscribe({
      next: () => this.router.navigate(['/fornecedores']),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao salvar fornecedor');
      },
    });
  }
}
```

```html
<!-- supplier-form.component.html -->
<div class="p-6 max-w-2xl">
  <h1 class="text-2xl font-bold text-gray-900 mb-6">
    {{ isEdit() ? 'Editar Fornecedor' : 'Novo Fornecedor' }}
  </h1>

  @if (error()) {
    <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
      {{ error() }}
    </div>
  }

  <form [formGroup]="form" (ngSubmit)="save()" class="space-y-4">
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Nome *</label>
      <input formControlName="name" type="text"
             class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">CNPJ/CPF</label>
      <input formControlName="document" type="text"
             class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
      <input formControlName="email" type="email"
             class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Telefone</label>
      <input formControlName="phone" type="text"
             class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
    </div>

    <div class="flex gap-3 pt-2">
      <button type="submit" [disabled]="form.invalid || loading()"
              class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed">
        {{ loading() ? 'Salvando...' : (isEdit() ? 'Salvar alteracoes' : 'Cadastrar') }}
      </button>
      <a routerLink="/fornecedores"
         class="px-4 py-2 border border-gray-200 text-gray-700 text-sm font-medium rounded-md hover:bg-gray-50">
        Cancelar
      </a>
    </div>
  </form>
</div>
```

**Step 5: Create routes**

```typescript
// suppliers.routes.ts
import { Routes } from '@angular/router';

export const suppliersRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/supplier-list/supplier-list.component').then((m) => m.SupplierListComponent),
  },
  {
    path: 'novo',
    loadComponent: () =>
      import('./pages/supplier-form/supplier-form.component').then((m) => m.SupplierFormComponent),
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('./pages/supplier-form/supplier-form.component').then((m) => m.SupplierFormComponent),
  },
];
```

**Step 6: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/suppliers/
git commit -m "feat(supplier): add frontend supplier feature (list, form, service, routes)"
```

---

## Task 13: Frontend — Client Feature

**Files:** Same structure as Task 12 but under `features/clients/`. Replace supplier→client, fornecedor→cliente.

- Create: `gestao-empresaial-frontend/src/app/features/clients/models/client.model.ts`
- Create: `gestao-empresaial-frontend/src/app/features/clients/services/client.service.ts`
- Create: `gestao-empresaial-frontend/src/app/features/clients/pages/client-list/client-list.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/clients/pages/client-list/client-list.component.html`
- Create: `gestao-empresaial-frontend/src/app/features/clients/pages/client-form/client-form.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/clients/pages/client-form/client-form.component.html`
- Create: `gestao-empresaial-frontend/src/app/features/clients/clients.routes.ts`

**Step 1:** Follow exact same structure as Task 12 with these replacements:
- `SupplierResponse` → `ClientResponse`
- `SupplierService` → `ClientService`
- `/api/suppliers` → `/api/clients`
- `SupplierListComponent` → `ClientListComponent`
- `SupplierFormComponent` → `ClientFormComponent`
- `suppliersRoutes` → `clientsRoutes`
- "Fornecedor(es)" → "Cliente(s)"
- `/fornecedores` → `/clientes`
- Route prefix: `fornecedores` → `clientes`

**Step 2: Create clients.routes.ts**

```typescript
import { Routes } from '@angular/router';

export const clientsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/client-list/client-list.component').then((m) => m.ClientListComponent),
  },
  {
    path: 'novo',
    loadComponent: () =>
      import('./pages/client-form/client-form.component').then((m) => m.ClientFormComponent),
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('./pages/client-form/client-form.component').then((m) => m.ClientFormComponent),
  },
];
```

**Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/clients/
git commit -m "feat(client): add frontend client feature (list, form, service, routes)"
```

---

## Task 14: Frontend — Categories Feature

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/categories/models/category.model.ts`
- Create: `gestao-empresaial-frontend/src/app/features/categories/services/category.service.ts`
- Create: `gestao-empresaial-frontend/src/app/features/categories/pages/category-management/category-management.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/categories/pages/category-management/category-management.component.html`
- Create: `gestao-empresaial-frontend/src/app/features/categories/categories.routes.ts`

**Step 1: Create model**

```typescript
// category.model.ts
export interface CategoryResponse {
  id: string;
  groupId: string;
  name: string;
  active: boolean;
}

export interface CategoryGroupResponse {
  id: string;
  name: string;
  type: 'REVENUE' | 'EXPENSE';
  displayOrder: number;
  categories: CategoryResponse[];
}

export interface CreateCategoryGroupRequest {
  name: string;
  type: 'REVENUE' | 'EXPENSE';
}

export interface UpdateCategoryGroupRequest {
  name: string;
}

export interface CreateCategoryRequest {
  groupId: string;
  name: string;
}

export interface UpdateCategoryRequest {
  name: string;
}
```

**Step 2: Create service**

```typescript
// category.service.ts
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  CategoryGroupResponse, CreateCategoryGroupRequest, UpdateCategoryGroupRequest,
  CategoryResponse, CreateCategoryRequest, UpdateCategoryRequest,
} from '../models/category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/categories';

  private readonly _groups = signal<CategoryGroupResponse[]>([]);
  readonly groups = this._groups.asReadonly();

  loadGroups(): Observable<CategoryGroupResponse[]> {
    return this.http.get<CategoryGroupResponse[]>(`${this.API_URL}/groups`).pipe(
      tap((groups) => this._groups.set(groups)),
    );
  }

  createGroup(data: CreateCategoryGroupRequest): Observable<CategoryGroupResponse> {
    return this.http.post<CategoryGroupResponse>(`${this.API_URL}/groups`, data);
  }

  updateGroup(id: string, data: UpdateCategoryGroupRequest): Observable<CategoryGroupResponse> {
    return this.http.put<CategoryGroupResponse>(`${this.API_URL}/groups/${id}`, data);
  }

  deleteGroup(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/groups/${id}`);
  }

  createCategory(data: CreateCategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(this.API_URL, data);
  }

  updateCategory(id: string, data: UpdateCategoryRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(`${this.API_URL}/${id}`, data);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
```

**Step 3: Create CategoryManagementComponent**

Single page showing groups as accordion sections with inline category management.

```typescript
// category-management.component.ts
import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CategoryService } from '../../services/category.service';
import { CategoryGroupResponse } from '../../models/category.model';

@Component({
  selector: 'app-category-management',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly categoryService = inject(CategoryService);

  protected readonly groups = this.categoryService.groups;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly addingCategoryToGroup = signal<string | null>(null);
  protected readonly showNewGroupForm = signal(false);

  protected readonly newGroupForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    type: ['EXPENSE', [Validators.required]],
  });

  protected readonly newCategoryForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
  });

  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    this.categoryService.loadGroups().subscribe({
      next: () => this.loading.set(false),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao carregar categorias');
      },
    });
  }

  protected createGroup() {
    if (this.newGroupForm.invalid) return;
    const { name, type } = this.newGroupForm.getRawValue();
    this.categoryService.createGroup({ name, type: type as 'REVENUE' | 'EXPENSE' }).subscribe({
      next: () => {
        this.newGroupForm.reset({ name: '', type: 'EXPENSE' });
        this.showNewGroupForm.set(false);
        this.loadData();
      },
      error: (err) => this.error.set(err.error?.message || 'Erro ao criar grupo'),
    });
  }

  protected deleteGroup(group: CategoryGroupResponse) {
    if (!confirm(`Excluir grupo "${group.name}"?`)) return;
    this.categoryService.deleteGroup(group.id).subscribe({
      next: () => this.loadData(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir grupo'),
    });
  }

  protected startAddCategory(groupId: string) {
    this.addingCategoryToGroup.set(groupId);
    this.newCategoryForm.reset({ name: '' });
  }

  protected addCategory(groupId: string) {
    if (this.newCategoryForm.invalid) return;
    const { name } = this.newCategoryForm.getRawValue();
    this.categoryService.createCategory({ groupId, name }).subscribe({
      next: () => {
        this.addingCategoryToGroup.set(null);
        this.loadData();
      },
      error: (err) => this.error.set(err.error?.message || 'Erro ao criar categoria'),
    });
  }

  protected deleteCategory(categoryId: string) {
    this.categoryService.deleteCategory(categoryId).subscribe({
      next: () => this.loadData(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir categoria'),
    });
  }

  protected typeLabel(type: string): string {
    return type === 'REVENUE' ? 'Receita' : 'Despesa';
  }
}
```

```html
<!-- category-management.component.html -->
<div class="p-6">
  <div class="flex items-center justify-between mb-6">
    <h1 class="text-2xl font-bold text-gray-900">Categorias</h1>
    <button (click)="showNewGroupForm.set(!showNewGroupForm())"
            class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700">
      Novo Grupo
    </button>
  </div>

  @if (error()) {
    <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
      {{ error() }}
    </div>
  }

  @if (showNewGroupForm()) {
    <div class="mb-6 p-4 border border-gray-200 rounded-lg">
      <h3 class="text-sm font-medium text-gray-700 mb-3">Novo Grupo de Categoria</h3>
      <form [formGroup]="newGroupForm" (ngSubmit)="createGroup()" class="flex gap-3 items-end">
        <div class="flex-1">
          <label class="block text-xs text-gray-500 mb-1">Nome</label>
          <input formControlName="name" type="text"
                 class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
        </div>
        <div>
          <label class="block text-xs text-gray-500 mb-1">Tipo</label>
          <select formControlName="type"
                  class="px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="EXPENSE">Despesa</option>
            <option value="REVENUE">Receita</option>
          </select>
        </div>
        <button type="submit" [disabled]="newGroupForm.invalid"
                class="px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 disabled:opacity-50">
          Criar
        </button>
        <button type="button" (click)="showNewGroupForm.set(false)"
                class="px-4 py-2 border border-gray-200 text-gray-700 text-sm rounded-md hover:bg-gray-50">
          Cancelar
        </button>
      </form>
    </div>
  }

  @if (loading()) {
    <div class="text-sm text-gray-500">Carregando...</div>
  } @else {
    <div class="space-y-4">
      @for (group of groups(); track group.id) {
        <div class="border border-gray-200 rounded-lg">
          <div class="flex items-center justify-between px-4 py-3 bg-gray-50 border-b border-gray-200">
            <div class="flex items-center gap-2">
              <h2 class="text-sm font-medium text-gray-900">{{ group.name }}</h2>
              <span class="text-xs px-2 py-0.5 rounded-full"
                    [class]="group.type === 'REVENUE'
                      ? 'bg-emerald-100 text-emerald-700'
                      : 'bg-red-100 text-red-700'">
                {{ typeLabel(group.type) }}
              </span>
            </div>
            <div class="flex gap-2">
              <button (click)="startAddCategory(group.id)"
                      class="text-blue-600 text-xs hover:underline">Adicionar categoria</button>
              <button (click)="deleteGroup(group)"
                      class="text-red-600 text-xs hover:underline">Excluir grupo</button>
            </div>
          </div>

          <div class="p-4">
            @if (addingCategoryToGroup() === group.id) {
              <form [formGroup]="newCategoryForm" (ngSubmit)="addCategory(group.id)"
                    class="flex gap-2 mb-3">
                <input formControlName="name" type="text" placeholder="Nome da categoria"
                       class="flex-1 px-3 py-1.5 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
                <button type="submit" [disabled]="newCategoryForm.invalid"
                        class="px-3 py-1.5 bg-blue-600 text-white text-xs rounded-md hover:bg-blue-700 disabled:opacity-50">
                  Salvar
                </button>
                <button type="button" (click)="addingCategoryToGroup.set(null)"
                        class="px-3 py-1.5 border border-gray-200 text-gray-700 text-xs rounded-md hover:bg-gray-50">
                  Cancelar
                </button>
              </form>
            }

            @if (group.categories.length === 0) {
              <p class="text-xs text-gray-400">Nenhuma categoria neste grupo.</p>
            } @else {
              <div class="flex flex-wrap gap-2">
                @for (cat of group.categories; track cat.id) {
                  <span class="inline-flex items-center gap-1 px-3 py-1 bg-gray-100 rounded-full text-sm text-gray-700">
                    {{ cat.name }}
                    <button (click)="deleteCategory(cat.id)"
                            class="text-gray-400 hover:text-red-600 ml-1"
                            title="Remover categoria">&times;</button>
                  </span>
                }
              </div>
            }
          </div>
        </div>
      }
    </div>
  }
</div>
```

**Step 4: Create routes**

```typescript
// categories.routes.ts
import { Routes } from '@angular/router';

export const categoriesRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/category-management/category-management.component').then(
        (m) => m.CategoryManagementComponent,
      ),
  },
];
```

**Step 5: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/categories/
git commit -m "feat(category): add frontend categories feature (management page, service, routes)"
```

---

## Task 15: Frontend — Route Registration + Sidebar Integration

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/app.routes.ts` (add supplier, client, category routes)
- Modify: `gestao-empresaial-frontend/src/app/core/layout/components/sidebar/sidebar.component.ts` or `.html` (update navigation links)

**Step 1: Add routes to app.routes.ts**

Inside the `MainLayoutComponent` children array, add:

```typescript
{
  path: 'fornecedores',
  loadChildren: () =>
    import('./features/suppliers/suppliers.routes').then((m) => m.suppliersRoutes),
},
{
  path: 'clientes',
  loadChildren: () =>
    import('./features/clients/clients.routes').then((m) => m.clientsRoutes),
},
{
  path: 'categorias',
  loadChildren: () =>
    import('./features/categories/categories.routes').then((m) => m.categoriesRoutes),
},
```

**Step 2: Update sidebar links**

Ensure the sidebar component has `routerLink="/fornecedores"`, `routerLink="/clientes"`, and `routerLink="/categorias"` links. The sidebar already has placeholder links for these — update them to use proper `routerLink` directives.

**Step 3: Verify frontend compiles**

Run: `cd gestao-empresaial-frontend && npm run build`

Expected: Build succeeds

**Step 4: Run all backend tests**

Run: `cd gestao-empresarial-backend && ./mvnw test -q`

Expected: All tests PASS

**Step 5: Commit**

```bash
git add gestao-empresaial-frontend/src/app/app.routes.ts
git add gestao-empresaial-frontend/src/app/core/layout/components/sidebar/
git commit -m "feat(routes): integrate supplier, client, category routes and sidebar navigation"
```

---

## Summary

| Task | Description | Backend | Frontend |
|------|-------------|---------|----------|
| 1 | Database migration (financial_schema) | ✅ | — |
| 2 | Supplier entity + repository | ✅ | — |
| 3 | Supplier DTOs + mapper | ✅ | — |
| 4 | Supplier service (TDD) | ✅ | — |
| 5 | Supplier controller | ✅ | — |
| 6 | Client entity + repo + DTOs + mapper | ✅ | — |
| 7 | Client service (TDD) + controller | ✅ | — |
| 8 | Category entities + repositories | ✅ | — |
| 9 | Category DTOs + mapper | ✅ | — |
| 10 | Category service (TDD) | ✅ | — |
| 11 | Category controller + seed integration | ✅ | — |
| 12 | Supplier frontend feature | — | ✅ |
| 13 | Client frontend feature | — | ✅ |
| 14 | Categories frontend feature | — | ✅ |
| 15 | Route registration + sidebar | — | ✅ |
