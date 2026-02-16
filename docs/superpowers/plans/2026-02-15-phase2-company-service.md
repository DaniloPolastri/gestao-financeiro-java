# Phase 2: Company Service + Multi-empresa — Implementation Plan

> **For Claude:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add company management with multi-tenancy — users create companies, invite members, switch between companies, and manage roles.

**Architecture:** Single-module Spring Boot 4 with layered architecture. New company entities in `company_schema`. Frontend adds CompanyService with signal-based active company, forced company creation after first login, and company selector dropdown in header.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, Flyway, MapStruct, PostgreSQL 17, Angular 21, Tailwind CSS 4, PrimeNG, Vitest

**Design Spec:** `docs/superpowers/specs/2026-02-15-phase2-company-service-design.md`

**Prerequisites:** Branch `feature/refactoring-structure` must be merged to master before starting.

---

## File Structure

### Backend — New Files

| File | Responsibility |
|------|---------------|
| `src/main/resources/db/migration/V2__create_company_tables.sql` | Flyway migration for company_schema tables |
| `src/main/java/com/findash/entity/MemberStatus.java` | Enum: ACTIVE, INVITED, REMOVED |
| `src/main/java/com/findash/entity/Company.java` | JPA entity for companies table |
| `src/main/java/com/findash/entity/CompanyMember.java` | JPA entity for company_members table |
| `src/main/java/com/findash/repository/CompanyRepository.java` | Spring Data repo for Company |
| `src/main/java/com/findash/repository/CompanyMemberRepository.java` | Spring Data repo for CompanyMember |
| `src/main/java/com/findash/exception/ForbiddenOperationException.java` | Exception for 403 responses |
| `src/main/java/com/findash/util/CnpjValidator.java` | CNPJ validation and normalization |
| `src/main/java/com/findash/dto/CreateCompanyRequestDTO.java` | Company creation request |
| `src/main/java/com/findash/dto/UpdateCompanyRequestDTO.java` | Company update request |
| `src/main/java/com/findash/dto/CompanyResponseDTO.java` | Company response with user's role |
| `src/main/java/com/findash/dto/InviteMemberRequestDTO.java` | Member invite request |
| `src/main/java/com/findash/dto/CompanyMemberResponseDTO.java` | Member list response |
| `src/main/java/com/findash/dto/UpdateMemberRoleRequestDTO.java` | Role change request |
| `src/main/java/com/findash/mapper/CompanyMapper.java` | MapStruct mapper (default methods) |
| `src/main/java/com/findash/service/CompanyService.java` | Service interface |
| `src/main/java/com/findash/service/impl/CompanyServiceImpl.java` | Service implementation |
| `src/main/java/com/findash/controller/CompanyController.java` | REST controller |

### Backend — Modified Files

| File | Change |
|------|--------|
| `src/main/java/com/findash/repository/UserRoleRepository.java` | Add `findByUserIdAndCompanyId` method |
| `src/main/java/com/findash/exception/GlobalExceptionHandler.java` | Add ForbiddenOperationException handler |
| `src/main/java/com/findash/service/impl/AuthServiceImpl.java` | Resolve pending invites on register |
| `src/main/resources/application.yml` | Add company_schema to Flyway schemas |

### Backend — Test Files

| File | Scope |
|------|-------|
| `src/test/java/com/findash/util/CnpjValidatorTest.java` | CNPJ validation logic |
| `src/test/java/com/findash/service/impl/CompanyServiceImplTest.java` | Company CRUD + member management |

### Frontend — New Files

| File | Responsibility |
|------|---------------|
| `src/app/core/services/company.service.ts` | Active company signal, CRUD, localStorage |
| `src/app/core/guards/company.guard.ts` | Redirects to /empresas/nova if no companies |
| `src/app/features/company/company.routes.ts` | Company feature routes |
| `src/app/features/company/pages/create-company/create-company.component.ts` | Create company form |
| `src/app/features/company/pages/create-company/create-company.component.html` | Create company template |
| `src/app/features/company/pages/company-settings/company-settings.component.ts` | Edit company |
| `src/app/features/company/pages/company-settings/company-settings.component.html` | Edit company template |
| `src/app/features/company/pages/user-management/user-management.component.ts` | Member management |
| `src/app/features/company/pages/user-management/user-management.component.html` | Member management template |

### Frontend — Modified Files

| File | Change |
|------|--------|
| `src/app/core/interceptors/auth.interceptor.ts` | Add X-Company-Id header |
| `src/app/layout/header/header.component.ts` | Inject CompanyService, add dropdown logic |
| `src/app/layout/header/header.component.html` | Company selector dropdown |
| `src/app/app.routes.ts` | Add company routes, companyGuard |

**Note:** `SecurityConfig.java` does NOT need modification — `anyRequest().authenticated()` already covers `/api/companies/**`.

---

## Chunk 1: Backend Data Layer

### Task 1: Create feature branch

- [ ] **Step 1: Create and switch to feature branch**

```bash
git checkout master
git pull origin master
git checkout -b feature/phase2-company-service
```

- [ ] **Step 2: Verify branch**

```bash
git branch --show-current
```
Expected: `feature/phase2-company-service`

---

### Task 2: Flyway migration

**Files:**
- Create: `gestao-empresarial-backend/src/main/resources/db/migration/V2__create_company_tables.sql`
- Modify: `gestao-empresarial-backend/src/main/resources/application.yml`

- [ ] **Step 1: Create V2 migration**

```sql
-- V2__create_company_tables.sql

CREATE TABLE company_schema.companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    cnpj VARCHAR(14) UNIQUE,
    segment VARCHAR(100),
    owner_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE company_schema.company_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES company_schema.companies(id) ON DELETE CASCADE,
    user_id UUID,
    invited_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INVITED', 'REMOVED')),
    invited_at TIMESTAMP NOT NULL DEFAULT NOW(),
    joined_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (company_id, invited_email)
);

CREATE INDEX idx_companies_owner_id ON company_schema.companies(owner_id);
CREATE INDEX idx_company_members_company_id ON company_schema.company_members(company_id);
CREATE INDEX idx_company_members_user_id ON company_schema.company_members(user_id);
CREATE INDEX idx_company_members_invited_email ON company_schema.company_members(invited_email);
```

- [ ] **Step 2: Update application.yml — add company_schema to Flyway**

In `gestao-empresarial-backend/src/main/resources/application.yml`, change:

```yaml
  flyway:
    schemas: auth_schema
```

To:

```yaml
  flyway:
    schemas: auth_schema,company_schema
```

- [ ] **Step 3: Verify migration runs**

```bash
cd gestao-empresarial-backend && ./mvnw flyway:info -Dflyway.configFiles=src/main/resources/application.yml
```

Or start the app and check logs for `Successfully applied 2 migration(s)`.

- [ ] **Step 4: Commit**

```bash
git add gestao-empresarial-backend/src/main/resources/db/migration/V2__create_company_tables.sql gestao-empresarial-backend/src/main/resources/application.yml
git commit -m "feat(company): add V2 migration for company_schema tables"
```

---

### Task 3: Entity classes and enum

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/entity/MemberStatus.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/entity/Company.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/entity/CompanyMember.java`

- [ ] **Step 1: Create MemberStatus enum**

```java
package com.findash.entity;

public enum MemberStatus {
    ACTIVE, INVITED, REMOVED
}
```

- [ ] **Step 2: Create Company entity**

Follow same pattern as `User.java` — extends `BaseEntity`, `@Table` with schema.

```java
package com.findash.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "companies", schema = "company_schema")
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String cnpj;

    private String segment;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private boolean active = true;

    public Company() {}

    public Company(String name, String cnpj, String segment, UUID ownerId) {
        this.name = name;
        this.cnpj = cnpj;
        this.segment = segment;
        this.ownerId = ownerId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

- [ ] **Step 3: Create CompanyMember entity**

```java
package com.findash.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_members", schema = "company_schema",
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "invited_email"}))
public class CompanyMember extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "invited_email", nullable = false)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "joined_at")
    private Instant joinedAt;

    public CompanyMember() {}

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getInvitedEmail() { return invitedEmail; }
    public void setInvitedEmail(String invitedEmail) { this.invitedEmail = invitedEmail; }
    public MemberStatus getStatus() { return status; }
    public void setStatus(MemberStatus status) { this.status = status; }
    public Instant getInvitedAt() { return invitedAt; }
    public void setInvitedAt(Instant invitedAt) { this.invitedAt = invitedAt; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
}
```

- [ ] **Step 4: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/entity/MemberStatus.java gestao-empresarial-backend/src/main/java/com/findash/entity/Company.java gestao-empresarial-backend/src/main/java/com/findash/entity/CompanyMember.java
git commit -m "feat(company): add Company, CompanyMember entities and MemberStatus enum"
```

---

### Task 4: Repositories

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/repository/CompanyRepository.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/repository/CompanyMemberRepository.java`
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/repository/UserRoleRepository.java`

- [ ] **Step 1: Create CompanyRepository**

```java
package com.findash.repository;

import com.findash.entity.Company;
import com.findash.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    boolean existsByCnpj(String cnpj);

    @Query("SELECT c FROM Company c WHERE c.id IN " +
           "(SELECT cm.companyId FROM CompanyMember cm WHERE cm.userId = :userId AND cm.status = :status)")
    List<Company> findByMemberUserIdAndStatus(UUID userId, MemberStatus status);
}
```

- [ ] **Step 2: Create CompanyMemberRepository**

```java
package com.findash.repository;

import com.findash.entity.CompanyMember;
import com.findash.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, UUID> {
    List<CompanyMember> findByCompanyIdAndStatusNot(UUID companyId, MemberStatus status);
    Optional<CompanyMember> findByCompanyIdAndUserId(UUID companyId, UUID userId);
    boolean existsByCompanyIdAndInvitedEmailAndStatusIn(UUID companyId, String email, List<MemberStatus> statuses);
    List<CompanyMember> findByInvitedEmailAndStatus(String email, MemberStatus status);
}
```

- [ ] **Step 3: Add findByUserIdAndCompanyId to UserRoleRepository**

In `gestao-empresarial-backend/src/main/java/com/findash/repository/UserRoleRepository.java`, add this method to the interface:

```java
    Optional<UserRole> findByUserIdAndCompanyId(UUID userId, UUID companyId);
```

Also add the import: `import java.util.Optional;`

- [ ] **Step 4: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/repository/CompanyRepository.java gestao-empresarial-backend/src/main/java/com/findash/repository/CompanyMemberRepository.java gestao-empresarial-backend/src/main/java/com/findash/repository/UserRoleRepository.java
git commit -m "feat(company): add CompanyRepository, CompanyMemberRepository, update UserRoleRepository"
```

---

### Task 5: ForbiddenOperationException + handler update

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/exception/ForbiddenOperationException.java`
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Create ForbiddenOperationException**

Follow same pattern as `BusinessRuleException.java`.

```java
package com.findash.exception;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Add handler to GlobalExceptionHandler**

In `gestao-empresarial-backend/src/main/java/com/findash/exception/GlobalExceptionHandler.java`, add this method after the `handleBusinessRule` method:

```java
    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(403, ex.getMessage()));
    }
```

- [ ] **Step 3: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/exception/ForbiddenOperationException.java gestao-empresarial-backend/src/main/java/com/findash/exception/GlobalExceptionHandler.java
git commit -m "feat(company): add ForbiddenOperationException with 403 handler"
```

---

### Task 6: CNPJ Validator with tests (TDD)

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/util/CnpjValidator.java`
- Create: `gestao-empresarial-backend/src/test/java/com/findash/util/CnpjValidatorTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.findash.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CnpjValidatorTest {

    @Test
    void isValid_withValidCnpj_returnsTrue() {
        assertTrue(CnpjValidator.isValid("11222333000181"));
    }

    @Test
    void isValid_withFormattedCnpj_returnsTrue() {
        assertTrue(CnpjValidator.isValid("11.222.333/0001-81"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000182", "12345678901234", "abcdefghijklmn", "123", ""})
    void isValid_withInvalidCnpj_returnsFalse(String cnpj) {
        assertFalse(CnpjValidator.isValid(cnpj));
    }

    @Test
    void isValid_withAllSameDigits_returnsFalse() {
        assertFalse(CnpjValidator.isValid("11111111111111"));
    }

    @Test
    void isValid_withNull_returnsFalse() {
        assertFalse(CnpjValidator.isValid(null));
    }

    @Test
    void normalize_removesFormatting() {
        assertEquals("11222333000181", CnpjValidator.normalize("11.222.333/0001-81"));
    }

    @Test
    void normalize_keepsRawDigits() {
        assertEquals("11222333000181", CnpjValidator.normalize("11222333000181"));
    }

    @Test
    void format_addsFormatting() {
        assertEquals("11.222.333/0001-81", CnpjValidator.format("11222333000181"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd gestao-empresarial-backend && ./mvnw test -Dtest=CnpjValidatorTest
```
Expected: FAIL — `CnpjValidator` class does not exist.

- [ ] **Step 3: Implement CnpjValidator**

```java
package com.findash.util;

public final class CnpjValidator {

    private CnpjValidator() {}

    public static String normalize(String cnpj) {
        if (cnpj == null) return null;
        return cnpj.replaceAll("[^0-9]", "");
    }

    public static String format(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) return cnpj;
        return cnpj.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    public static boolean isValid(String cnpj) {
        if (cnpj == null) return false;
        String digits = normalize(cnpj);
        if (digits.length() != 14) return false;
        if (digits.chars().distinct().count() == 1) return false;

        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights1[i];
        }
        int remainder = sum % 11;
        int check1 = remainder < 2 ? 0 : 11 - remainder;
        if (Character.getNumericValue(digits.charAt(12)) != check1) return false;

        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights2[i];
        }
        remainder = sum % 11;
        int check2 = remainder < 2 ? 0 : 11 - remainder;
        return Character.getNumericValue(digits.charAt(13)) == check2;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd gestao-empresarial-backend && ./mvnw test -Dtest=CnpjValidatorTest
```
Expected: All 8 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/util/CnpjValidator.java gestao-empresarial-backend/src/test/java/com/findash/util/CnpjValidatorTest.java
git commit -m "feat(company): add CnpjValidator with TDD tests"
```

---

## Chunk 2: Backend Business Layer

### Task 7: DTOs

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/CreateCompanyRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/UpdateCompanyRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/CompanyResponseDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/InviteMemberRequestDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/CompanyMemberResponseDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/UpdateMemberRoleRequestDTO.java`

- [ ] **Step 1: Create all DTOs**

Follow same pattern as existing DTOs — Java records with Jakarta validation.

```java
// CreateCompanyRequestDTO.java
package com.findash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name,
    String cnpj,
    String segment
) {}
```

```java
// UpdateCompanyRequestDTO.java
package com.findash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequestDTO(
    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name,
    String cnpj,
    String segment
) {}
```

```java
// CompanyResponseDTO.java
package com.findash.dto;

import java.util.UUID;

public record CompanyResponseDTO(
    UUID id,
    String name,
    String cnpj,
    String segment,
    UUID ownerId,
    String ownerName,
    String role,
    boolean active
) {}
```

```java
// InviteMemberRequestDTO.java
package com.findash.dto;

import com.findash.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequestDTO(
    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email,
    @NotNull(message = "Role e obrigatoria")
    Role role
) {}
```

```java
// CompanyMemberResponseDTO.java
package com.findash.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyMemberResponseDTO(
    UUID userId,
    String name,
    String email,
    String role,
    String status,
    Instant joinedAt
) {}
```

```java
// UpdateMemberRoleRequestDTO.java
package com.findash.dto;

import com.findash.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequestDTO(
    @NotNull(message = "Role e obrigatoria") Role role
) {}
```

- [ ] **Step 2: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/dto/CreateCompanyRequestDTO.java gestao-empresarial-backend/src/main/java/com/findash/dto/UpdateCompanyRequestDTO.java gestao-empresarial-backend/src/main/java/com/findash/dto/CompanyResponseDTO.java gestao-empresarial-backend/src/main/java/com/findash/dto/InviteMemberRequestDTO.java gestao-empresarial-backend/src/main/java/com/findash/dto/CompanyMemberResponseDTO.java gestao-empresarial-backend/src/main/java/com/findash/dto/UpdateMemberRoleRequestDTO.java
git commit -m "feat(company): add Company DTOs"
```

---

### Task 8: CompanyMapper

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/mapper/CompanyMapper.java`

- [ ] **Step 1: Create CompanyMapper**

Uses default methods (not MapStruct `@Mapping` annotations) because DTOs need data from multiple sources (Company entity + UserRole + User).

```java
package com.findash.mapper;

import com.findash.dto.CompanyMemberResponseDTO;
import com.findash.dto.CompanyResponseDTO;
import com.findash.entity.Company;
import com.findash.entity.CompanyMember;
import com.findash.util.CnpjValidator;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    default CompanyResponseDTO toCompanyResponse(Company company, String role, String ownerName) {
        return new CompanyResponseDTO(
            company.getId(),
            company.getName(),
            CnpjValidator.format(company.getCnpj()),
            company.getSegment(),
            company.getOwnerId(),
            ownerName,
            role,
            company.isActive()
        );
    }

    default CompanyMemberResponseDTO toMemberResponse(CompanyMember member, String name,
                                                       String email, String role) {
        return new CompanyMemberResponseDTO(
            member.getUserId(),
            name,
            email,
            role,
            member.getStatus().name(),
            member.getJoinedAt()
        );
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/mapper/CompanyMapper.java
git commit -m "feat(company): add CompanyMapper"
```

---

### Task 9: CompanyService interface

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/service/CompanyService.java`

- [ ] **Step 1: Create CompanyService interface**

```java
package com.findash.service;

import com.findash.dto.*;

import java.util.List;
import java.util.UUID;

public interface CompanyService {
    CompanyResponseDTO createCompany(CreateCompanyRequestDTO request, UUID userId);
    List<CompanyResponseDTO> listUserCompanies(UUID userId);
    CompanyResponseDTO getCompany(UUID companyId, UUID userId);
    CompanyResponseDTO updateCompany(UUID companyId, UpdateCompanyRequestDTO request, UUID userId);
    List<CompanyMemberResponseDTO> listMembers(UUID companyId, UUID userId);
    CompanyMemberResponseDTO inviteMember(UUID companyId, InviteMemberRequestDTO request, UUID userId);
    void updateMemberRole(UUID companyId, UUID memberId, UpdateMemberRoleRequestDTO request, UUID userId);
    void removeMember(UUID companyId, UUID memberId, UUID userId);
}
```

- [ ] **Step 2: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/service/CompanyService.java
git commit -m "feat(company): add CompanyService interface"
```

---

### Task 10: CompanyServiceImpl — company CRUD with tests (TDD)

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/service/impl/CompanyServiceImpl.java`
- Create: `gestao-empresarial-backend/src/test/java/com/findash/service/impl/CompanyServiceImplTest.java`

- [ ] **Step 1: Write failing tests for company CRUD**

```java
package com.findash.service.impl;

import com.findash.dto.*;
import com.findash.entity.*;
import com.findash.exception.BusinessRuleException;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ForbiddenOperationException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.CompanyMapper;
import com.findash.repository.CompanyMemberRepository;
import com.findash.repository.CompanyRepository;
import com.findash.repository.UserRepository;
import com.findash.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private CompanyMemberRepository companyMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private CompanyMapper companyMapper;

    private CompanyServiceImpl service;

    private UUID userId;
    private UUID companyId;
    private User testUser;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        service = new CompanyServiceImpl(companyRepository, companyMemberRepository,
                userRepository, userRoleRepository, companyMapper);
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        testUser = new User("Test User", "test@email.com", "hashed");
        testUser.setId(userId);
        testCompany = new Company("Test Company", null, null, userId);
        testCompany.setId(companyId);
    }

    // --- createCompany ---

    @Test
    void createCompany_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(companyRepository.save(any())).thenAnswer(i -> {
            Company c = i.getArgument(0);
            c.setId(companyId);
            return c;
        });
        when(companyMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRoleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(companyMapper.toCompanyResponse(any(), eq("ADMIN"), eq("Test User")))
                .thenReturn(new CompanyResponseDTO(companyId, "Test Company", null, null,
                        userId, "Test User", "ADMIN", true));

        CompanyResponseDTO result = service.createCompany(
                new CreateCompanyRequestDTO("Test Company", null, null), userId);

        assertNotNull(result);
        assertEquals("Test Company", result.name());
        assertEquals("ADMIN", result.role());
        verify(companyRepository).save(any());
        verify(companyMemberRepository).save(any());
        verify(userRoleRepository).save(any());
    }

    @Test
    void createCompany_withInvalidCnpj_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        assertThrows(BusinessRuleException.class,
                () -> service.createCompany(
                        new CreateCompanyRequestDTO("Test", "12345678901234", null), userId));
    }

    @Test
    void createCompany_withDuplicateCnpj_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(companyRepository.existsByCnpj("11222333000181")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.createCompany(
                        new CreateCompanyRequestDTO("Test", "11222333000181", null), userId));
    }

    // --- listUserCompanies ---

    @Test
    void listUserCompanies_returnsCompaniesWithRoles() {
        when(companyRepository.findByMemberUserIdAndStatus(userId, MemberStatus.ACTIVE))
                .thenReturn(List.of(testCompany));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        UserRole ur = new UserRole();
        ur.setRole(Role.ADMIN);
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(ur));
        when(companyMapper.toCompanyResponse(testCompany, "ADMIN", "Test User"))
                .thenReturn(new CompanyResponseDTO(companyId, "Test Company", null, null,
                        userId, "Test User", "ADMIN", true));

        List<CompanyResponseDTO> result = service.listUserCompanies(userId);

        assertEquals(1, result.size());
        assertEquals("Test Company", result.getFirst().name());
    }

    // --- getCompany ---

    @Test
    void getCompany_success() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(new UserRole()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(companyMapper.toCompanyResponse(any(), any(), any()))
                .thenReturn(new CompanyResponseDTO(companyId, "Test Company", null, null,
                        userId, "Test User", "ADMIN", true));

        CompanyResponseDTO result = service.getCompany(companyId, userId);
        assertNotNull(result);
    }

    @Test
    void getCompany_notMember_throwsForbidden() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class,
                () -> service.getCompany(companyId, userId));
    }

    // --- updateCompany ---

    @Test
    void updateCompany_asAdmin_success() {
        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(companyMapper.toCompanyResponse(any(), eq("ADMIN"), eq("Test User")))
                .thenReturn(new CompanyResponseDTO(companyId, "Updated", null, null,
                        userId, "Test User", "ADMIN", true));

        CompanyResponseDTO result = service.updateCompany(companyId,
                new UpdateCompanyRequestDTO("Updated", null, null), userId);

        assertNotNull(result);
        verify(companyRepository).save(any());
    }

    @Test
    void updateCompany_asEditor_throwsForbidden() {
        UserRole editorRole = new UserRole();
        editorRole.setRole(Role.EDITOR);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(editorRole));

        assertThrows(ForbiddenOperationException.class,
                () -> service.updateCompany(companyId,
                        new UpdateCompanyRequestDTO("Updated", null, null), userId));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd gestao-empresarial-backend && ./mvnw test -Dtest=CompanyServiceImplTest
```
Expected: FAIL — `CompanyServiceImpl` class does not exist.

- [ ] **Step 3: Implement CompanyServiceImpl — company CRUD methods**

```java
package com.findash.service.impl;

import com.findash.dto.*;
import com.findash.entity.*;
import com.findash.exception.BusinessRuleException;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ForbiddenOperationException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.CompanyMapper;
import com.findash.repository.CompanyMemberRepository;
import com.findash.repository.CompanyRepository;
import com.findash.repository.UserRepository;
import com.findash.repository.UserRoleRepository;
import com.findash.service.CompanyService;
import com.findash.util.CnpjValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final CompanyMapper mapper;

    public CompanyServiceImpl(CompanyRepository companyRepository,
                              CompanyMemberRepository companyMemberRepository,
                              UserRepository userRepository,
                              UserRoleRepository userRoleRepository,
                              CompanyMapper mapper) {
        this.companyRepository = companyRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public CompanyResponseDTO createCompany(CreateCompanyRequestDTO request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String normalizedCnpj = null;
        if (request.cnpj() != null && !request.cnpj().isBlank()) {
            normalizedCnpj = CnpjValidator.normalize(request.cnpj());
            if (!CnpjValidator.isValid(normalizedCnpj)) {
                throw new BusinessRuleException("CNPJ invalido");
            }
            if (companyRepository.existsByCnpj(normalizedCnpj)) {
                throw new DuplicateResourceException("CNPJ ja cadastrado: " + request.cnpj());
            }
        }

        Company company = new Company(request.name(), normalizedCnpj, request.segment(), userId);
        company = companyRepository.save(company);

        CompanyMember member = new CompanyMember();
        member.setCompanyId(company.getId());
        member.setUserId(userId);
        member.setInvitedEmail(user.getEmail());
        member.setStatus(MemberStatus.ACTIVE);
        member.setInvitedAt(Instant.now());
        member.setJoinedAt(Instant.now());
        companyMemberRepository.save(member);

        UserRole role = new UserRole();
        role.setUserId(userId);
        role.setCompanyId(company.getId());
        role.setRole(Role.ADMIN);
        userRoleRepository.save(role);

        return mapper.toCompanyResponse(company, "ADMIN", user.getName());
    }

    @Override
    public List<CompanyResponseDTO> listUserCompanies(UUID userId) {
        List<Company> companies = companyRepository.findByMemberUserIdAndStatus(userId, MemberStatus.ACTIVE);

        return companies.stream().map(company -> {
            UserRole userRole = userRoleRepository.findByUserIdAndCompanyId(userId, company.getId())
                    .orElse(null);
            String role = userRole != null ? userRole.getRole().name() : null;

            User owner = userRepository.findById(company.getOwnerId()).orElse(null);
            String ownerName = owner != null ? owner.getName() : null;

            return mapper.toCompanyResponse(company, role, ownerName);
        }).toList();
    }

    @Override
    public CompanyResponseDTO getCompany(UUID companyId, UUID userId) {
        Company company = findCompanyOrThrow(companyId);
        UserRole userRole = requireMembership(userId, companyId);

        User owner = userRepository.findById(company.getOwnerId()).orElse(null);
        String ownerName = owner != null ? owner.getName() : null;

        return mapper.toCompanyResponse(company, userRole.getRole().name(), ownerName);
    }

    @Override
    @Transactional
    public CompanyResponseDTO updateCompany(UUID companyId, UpdateCompanyRequestDTO request, UUID userId) {
        Company company = findCompanyOrThrow(companyId);
        requireAdmin(userId, companyId);

        String normalizedCnpj = null;
        if (request.cnpj() != null && !request.cnpj().isBlank()) {
            normalizedCnpj = CnpjValidator.normalize(request.cnpj());
            if (!CnpjValidator.isValid(normalizedCnpj)) {
                throw new BusinessRuleException("CNPJ invalido");
            }
            if (!normalizedCnpj.equals(company.getCnpj()) && companyRepository.existsByCnpj(normalizedCnpj)) {
                throw new DuplicateResourceException("CNPJ ja cadastrado: " + request.cnpj());
            }
        }

        company.setName(request.name());
        company.setCnpj(normalizedCnpj);
        company.setSegment(request.segment());
        company = companyRepository.save(company);

        User owner = userRepository.findById(company.getOwnerId()).orElse(null);
        String ownerName = owner != null ? owner.getName() : null;

        return mapper.toCompanyResponse(company, "ADMIN", ownerName);
    }

    // --- Member management (Task 11) ---

    @Override
    public List<CompanyMemberResponseDTO> listMembers(UUID companyId, UUID userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public CompanyMemberResponseDTO inviteMember(UUID companyId, InviteMemberRequestDTO request, UUID userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void updateMemberRole(UUID companyId, UUID memberId, UpdateMemberRoleRequestDTO request, UUID userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    @Transactional
    public void removeMember(UUID companyId, UUID memberId, UUID userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // --- Helper methods ---

    private Company findCompanyOrThrow(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
    }

    private UserRole requireMembership(UUID userId, UUID companyId) {
        return userRoleRepository.findByUserIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ForbiddenOperationException(
                        "Voce nao e membro desta empresa"));
    }

    private UserRole requireAdmin(UUID userId, UUID companyId) {
        UserRole role = requireMembership(userId, companyId);
        if (role.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException(
                    "Apenas administradores podem realizar esta operacao");
        }
        return role;
    }
}
```

- [ ] **Step 4: Run tests to verify CRUD tests pass**

```bash
cd gestao-empresarial-backend && ./mvnw test -Dtest=CompanyServiceImplTest
```
Expected: All 7 CRUD tests PASS. (Member management tests not yet written.)

- [ ] **Step 5: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/service/impl/CompanyServiceImpl.java gestao-empresarial-backend/src/test/java/com/findash/service/impl/CompanyServiceImplTest.java
git commit -m "feat(company): add CompanyServiceImpl with company CRUD + tests"
```

---

### Task 11: CompanyServiceImpl — member management with tests (TDD)

**Files:**
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/service/impl/CompanyServiceImpl.java`
- Modify: `gestao-empresarial-backend/src/test/java/com/findash/service/impl/CompanyServiceImplTest.java`

- [ ] **Step 1: Add member management tests to CompanyServiceImplTest**

Append these tests to the existing `CompanyServiceImplTest` class:

```java
    // --- inviteMember ---

    @Test
    void inviteMember_existingUser_activatesImmediately() {
        User invitedUser = new User("Invited", "invited@email.com", "hashed");
        invitedUser.setId(UUID.randomUUID());

        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyMemberRepository.existsByCompanyIdAndInvitedEmailAndStatusIn(
                eq(companyId), eq("invited@email.com"), anyList())).thenReturn(false);
        when(userRepository.findByEmail("invited@email.com")).thenReturn(Optional.of(invitedUser));
        when(companyMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRoleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(companyMapper.toMemberResponse(any(), eq("Invited"), eq("invited@email.com"), eq("EDITOR")))
                .thenReturn(new CompanyMemberResponseDTO(invitedUser.getId(), "Invited",
                        "invited@email.com", "EDITOR", "ACTIVE", Instant.now()));

        CompanyMemberResponseDTO result = service.inviteMember(companyId,
                new InviteMemberRequestDTO("invited@email.com", Role.EDITOR), userId);

        assertEquals("ACTIVE", result.status());
        verify(userRoleRepository).save(any());
    }

    @Test
    void inviteMember_nonExistingUser_createsInvitedStatus() {
        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyMemberRepository.existsByCompanyIdAndInvitedEmailAndStatusIn(
                eq(companyId), eq("new@email.com"), anyList())).thenReturn(false);
        when(userRepository.findByEmail("new@email.com")).thenReturn(Optional.empty());
        when(companyMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(companyMapper.toMemberResponse(any(), isNull(), eq("new@email.com"), isNull()))
                .thenReturn(new CompanyMemberResponseDTO(null, null,
                        "new@email.com", null, "INVITED", null));

        CompanyMemberResponseDTO result = service.inviteMember(companyId,
                new InviteMemberRequestDTO("new@email.com", Role.EDITOR), userId);

        assertEquals("INVITED", result.status());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void inviteMember_duplicateInvite_throwsException() {
        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyMemberRepository.existsByCompanyIdAndInvitedEmailAndStatusIn(
                eq(companyId), eq("dup@email.com"), anyList())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.inviteMember(companyId,
                        new InviteMemberRequestDTO("dup@email.com", Role.EDITOR), userId));
    }

    @Test
    void inviteMember_asAdmin_throwsException() {
        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));

        assertThrows(BusinessRuleException.class,
                () -> service.inviteMember(companyId,
                        new InviteMemberRequestDTO("test@email.com", Role.ADMIN), userId));
    }

    // --- updateMemberRole ---

    @Test
    void updateMemberRole_success() {
        UUID memberId = UUID.randomUUID();
        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);
        UserRole memberRole = new UserRole();
        memberRole.setRole(Role.EDITOR);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(userRoleRepository.findByUserIdAndCompanyId(memberId, companyId))
                .thenReturn(Optional.of(memberRole));
        when(userRoleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateMemberRole(companyId, memberId,
                new UpdateMemberRoleRequestDTO(Role.VIEWER), userId);

        verify(userRoleRepository).save(argThat(ur -> ur.getRole() == Role.VIEWER));
    }

    @Test
    void updateMemberRole_owner_throwsForbidden() {
        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));

        // userId is the owner (testCompany.ownerId == userId)
        assertThrows(ForbiddenOperationException.class,
                () -> service.updateMemberRole(companyId, userId,
                        new UpdateMemberRoleRequestDTO(Role.EDITOR), userId));
    }

    @Test
    void updateMemberRole_lastAdmin_throwsException() {
        UUID memberId = UUID.randomUUID();
        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);
        UserRole targetAdminRole = new UserRole();
        targetAdminRole.setRole(Role.ADMIN);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(userRoleRepository.findByUserIdAndCompanyId(memberId, companyId))
                .thenReturn(Optional.of(targetAdminRole));
        when(userRoleRepository.countByCompanyIdAndRole(companyId, Role.ADMIN)).thenReturn(1L);

        assertThrows(BusinessRuleException.class,
                () -> service.updateMemberRole(companyId, memberId,
                        new UpdateMemberRoleRequestDTO(Role.EDITOR), userId));
    }

    // --- removeMember ---

    @Test
    void removeMember_success() {
        UUID memberId = UUID.randomUUID();
        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);
        CompanyMember member = new CompanyMember();
        member.setUserId(memberId);
        member.setStatus(MemberStatus.ACTIVE);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));
        when(companyMemberRepository.findByCompanyIdAndUserId(companyId, memberId))
                .thenReturn(Optional.of(member));
        when(userRoleRepository.countByCompanyIdAndRole(companyId, Role.ADMIN)).thenReturn(2L);

        service.removeMember(companyId, memberId, userId);

        verify(companyMemberRepository).save(argThat(m -> m.getStatus() == MemberStatus.REMOVED));
        verify(userRoleRepository).deleteByUserIdAndCompanyId(memberId, companyId);
    }

    @Test
    void removeMember_owner_throwsForbidden() {
        UserRole adminRole = new UserRole();
        adminRole.setRole(Role.ADMIN);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(testCompany));
        when(userRoleRepository.findByUserIdAndCompanyId(userId, companyId))
                .thenReturn(Optional.of(adminRole));

        // userId is the owner
        assertThrows(ForbiddenOperationException.class,
                () -> service.removeMember(companyId, userId, userId));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd gestao-empresarial-backend && ./mvnw test -Dtest=CompanyServiceImplTest
```
Expected: New tests FAIL with `UnsupportedOperationException`.

- [ ] **Step 3: Replace the stub member management methods in CompanyServiceImpl**

Replace the four `throw new UnsupportedOperationException("Not yet implemented")` methods with:

```java
    @Override
    public List<CompanyMemberResponseDTO> listMembers(UUID companyId, UUID userId) {
        findCompanyOrThrow(companyId);
        UserRole userRole = requireMembership(userId, companyId);
        if (userRole.getRole() == Role.VIEWER) {
            throw new ForbiddenOperationException("Viewers nao podem ver membros");
        }

        List<CompanyMember> members = companyMemberRepository
                .findByCompanyIdAndStatusNot(companyId, MemberStatus.REMOVED);

        return members.stream().map(member -> {
            if (member.getUserId() != null) {
                User memberUser = userRepository.findById(member.getUserId()).orElse(null);
                UserRole memberRole = userRoleRepository
                        .findByUserIdAndCompanyId(member.getUserId(), companyId).orElse(null);
                return mapper.toMemberResponse(member,
                        memberUser != null ? memberUser.getName() : null,
                        memberUser != null ? memberUser.getEmail() : member.getInvitedEmail(),
                        memberRole != null ? memberRole.getRole().name() : null);
            } else {
                return mapper.toMemberResponse(member, null, member.getInvitedEmail(), null);
            }
        }).toList();
    }

    @Override
    @Transactional
    public CompanyMemberResponseDTO inviteMember(UUID companyId, InviteMemberRequestDTO request, UUID userId) {
        findCompanyOrThrow(companyId);
        requireAdmin(userId, companyId);

        if (request.role() == Role.ADMIN) {
            throw new BusinessRuleException("Nao e possivel convidar como ADMIN");
        }

        if (companyMemberRepository.existsByCompanyIdAndInvitedEmailAndStatusIn(
                companyId, request.email(), List.of(MemberStatus.ACTIVE, MemberStatus.INVITED))) {
            throw new DuplicateResourceException("Usuario ja convidado ou membro desta empresa");
        }

        CompanyMember member = new CompanyMember();
        member.setCompanyId(companyId);
        member.setInvitedEmail(request.email());
        member.setInvitedAt(Instant.now());

        var existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            User invitedUser = existingUser.get();
            member.setUserId(invitedUser.getId());
            member.setStatus(MemberStatus.ACTIVE);
            member.setJoinedAt(Instant.now());
            companyMemberRepository.save(member);

            UserRole role = new UserRole();
            role.setUserId(invitedUser.getId());
            role.setCompanyId(companyId);
            role.setRole(request.role());
            userRoleRepository.save(role);

            return mapper.toMemberResponse(member, invitedUser.getName(),
                    invitedUser.getEmail(), request.role().name());
        } else {
            member.setStatus(MemberStatus.INVITED);
            companyMemberRepository.save(member);
            return mapper.toMemberResponse(member, null, request.email(), null);
        }
    }

    @Override
    @Transactional
    public void updateMemberRole(UUID companyId, UUID memberId, UpdateMemberRoleRequestDTO request, UUID userId) {
        Company company = findCompanyOrThrow(companyId);
        requireAdmin(userId, companyId);

        if (company.getOwnerId().equals(memberId)) {
            throw new ForbiddenOperationException("Nao e possivel alterar o role do proprietario");
        }

        UserRole memberRole = userRoleRepository.findByUserIdAndCompanyId(memberId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        if (memberRole.getRole() == Role.ADMIN && request.role() != Role.ADMIN) {
            long adminCount = userRoleRepository.countByCompanyIdAndRole(companyId, Role.ADMIN);
            if (adminCount <= 1) {
                throw new BusinessRuleException("Nao e possivel alterar o role do ultimo administrador");
            }
        }

        memberRole.setRole(request.role());
        userRoleRepository.save(memberRole);
    }

    @Override
    @Transactional
    public void removeMember(UUID companyId, UUID memberId, UUID userId) {
        Company company = findCompanyOrThrow(companyId);
        requireAdmin(userId, companyId);

        if (company.getOwnerId().equals(memberId)) {
            throw new ForbiddenOperationException("Nao e possivel remover o proprietario");
        }

        UserRole memberRole = userRoleRepository.findByUserIdAndCompanyId(memberId, companyId)
                .orElse(null);
        if (memberRole != null && memberRole.getRole() == Role.ADMIN) {
            long adminCount = userRoleRepository.countByCompanyIdAndRole(companyId, Role.ADMIN);
            if (adminCount <= 1) {
                throw new BusinessRuleException("Nao e possivel remover o ultimo administrador");
            }
        }

        CompanyMember member = companyMemberRepository.findByCompanyIdAndUserId(companyId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyMember", memberId));

        member.setStatus(MemberStatus.REMOVED);
        companyMemberRepository.save(member);
        userRoleRepository.deleteByUserIdAndCompanyId(memberId, companyId);
    }
```

- [ ] **Step 4: Run all tests to verify they pass**

```bash
cd gestao-empresarial-backend && ./mvnw test -Dtest=CompanyServiceImplTest
```
Expected: All 17 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/service/impl/CompanyServiceImpl.java gestao-empresarial-backend/src/test/java/com/findash/service/impl/CompanyServiceImplTest.java
git commit -m "feat(company): add member management (invite, role change, remove) + tests"
```

---

## Chunk 3: Backend Web Layer + Auth Integration

### Task 12: CompanyController

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/controller/CompanyController.java`

- [ ] **Step 1: Create CompanyController**

Follow same pattern as `AuthController.java` — constructor injection, `@RestController`, `@RequestMapping`.

```java
package com.findash.controller;

import com.findash.dto.*;
import com.findash.security.UserContext;
import com.findash.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CompanyResponseDTO> createCompany(
            @Valid @RequestBody CreateCompanyRequestDTO request,
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createCompany(request, user.userId()));
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponseDTO>> listCompanies(
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(service.listUserCompanies(user.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> getCompany(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(service.getCompany(id, user.userId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponseDTO> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequestDTO request,
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(service.updateCompany(id, request, user.userId()));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<CompanyMemberResponseDTO>> listMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(service.listMembers(id, user.userId()));
    }

    @PostMapping("/{id}/members/invite")
    public ResponseEntity<CompanyMemberResponseDTO> inviteMember(
            @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequestDTO request,
            @AuthenticationPrincipal UserContext user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.inviteMember(id, request, user.userId()));
    }

    @PutMapping("/{id}/members/{memberId}/role")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRoleRequestDTO request,
            @AuthenticationPrincipal UserContext user) {
        service.updateMemberRole(id, memberId, request, user.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserContext user) {
        service.removeMember(id, memberId, user.userId());
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/controller/CompanyController.java
git commit -m "feat(company): add CompanyController with all endpoints"
```

---

### Task 13: AuthServiceImpl — resolve pending invites on register

**Files:**
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/service/impl/AuthServiceImpl.java`
- Modify: `gestao-empresarial-backend/src/test/java/com/findash/service/impl/AuthServiceImplTest.java`

- [ ] **Step 1: Add test for pending invite resolution**

In `AuthServiceImplTest.java`, add a new `@Mock` and update the constructor:

First, add the import and mock field:
```java
import com.findash.entity.CompanyMember;
import com.findash.entity.MemberStatus;
import com.findash.entity.Role;
import com.findash.entity.UserRole;
import com.findash.repository.CompanyMemberRepository;
```

Add mock:
```java
    @Mock private CompanyMemberRepository companyMemberRepository;
```

Update `setUp()`:
```java
    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, userRoleRepository,
                refreshTokenRepository, companyMemberRepository,
                tokenProvider, mapper, 2592000000L);
    }
```

Add test:
```java
    @Test
    void register_withPendingInvites_resolvesInvites() {
        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(userRoleRepository.findByUserId(any())).thenReturn(List.of());
        when(tokenProvider.generateAccessToken(any(), anyString(), anyList())).thenReturn("jwt");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toAuthResponse(anyString(), anyString(), any()))
                .thenReturn(new AuthResponseDTO("jwt", "refresh",
                        new UserResponseDTO(UUID.randomUUID(), "New", "new@email.com")));

        // Setup pending invite
        CompanyMember pendingInvite = new CompanyMember();
        pendingInvite.setCompanyId(UUID.randomUUID());
        pendingInvite.setInvitedEmail("new@email.com");
        pendingInvite.setStatus(MemberStatus.INVITED);
        when(companyMemberRepository.findByInvitedEmailAndStatus("new@email.com", MemberStatus.INVITED))
                .thenReturn(List.of(pendingInvite));
        when(companyMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        authService.register(new RegisterRequestDTO("New", "new@email.com", "password"));

        verify(companyMemberRepository).save(argThat(m ->
                m.getStatus() == MemberStatus.ACTIVE && m.getUserId() != null));
        // Verify UserRole created for the invite (2 saves: 1 existing + 1 for invite)
        verify(userRoleRepository, atLeast(1)).save(any());
    }
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd gestao-empresarial-backend && ./mvnw test -Dtest=AuthServiceImplTest#register_withPendingInvites_resolvesInvites
```
Expected: FAIL — `AuthServiceImpl` constructor doesn't accept `CompanyMemberRepository`.

- [ ] **Step 3: Modify AuthServiceImpl to accept CompanyMemberRepository and resolve invites**

In `gestao-empresarial-backend/src/main/java/com/findash/service/impl/AuthServiceImpl.java`:

Add import:
```java
import com.findash.entity.MemberStatus;
import com.findash.entity.Role;
import com.findash.entity.UserRole;
import com.findash.repository.CompanyMemberRepository;
```

Add field:
```java
    private final CompanyMemberRepository companyMemberRepository;
```

Update constructor to add `CompanyMemberRepository companyMemberRepository` parameter (insert after `refreshTokenRepository`):
```java
    public AuthServiceImpl(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           CompanyMemberRepository companyMemberRepository,
                           JwtTokenProvider tokenProvider,
                           AuthMapper mapper,
                           @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.companyMemberRepository = companyMemberRepository;
        this.tokenProvider = tokenProvider;
        this.mapper = mapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }
```

In the `register()` method, after `user = userRepository.save(user);`, add invite resolution before `return createAuthResponse(user);`:

```java
        // Resolve pending invites
        var pendingInvites = companyMemberRepository
                .findByInvitedEmailAndStatus(request.email(), MemberStatus.INVITED);
        for (var invite : pendingInvites) {
            invite.setUserId(user.getId());
            invite.setStatus(MemberStatus.ACTIVE);
            invite.setJoinedAt(Instant.now());
            companyMemberRepository.save(invite);

            UserRole role = new UserRole();
            role.setUserId(user.getId());
            role.setCompanyId(invite.getCompanyId());
            role.setRole(Role.EDITOR); // default role for invited users
            userRoleRepository.save(role);
        }
```

**Important:** Also add `import java.time.Instant;` if not already present.

- [ ] **Step 4: Update existing AuthServiceImplTest setUp to pass companyMemberRepository**

In the existing `AuthServiceImplTest.java`, the `@BeforeEach setUp()` must be updated. Also add `lenient()` for `companyMemberRepository` in tests that don't involve invites:

Update setUp:
```java
    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, userRoleRepository,
                refreshTokenRepository, companyMemberRepository,
                tokenProvider, mapper, 2592000000L);
    }
```

In `register_withNewEmail_createsUser()`, add after existing stubs:
```java
        when(companyMemberRepository.findByInvitedEmailAndStatus("new@email.com", MemberStatus.INVITED))
                .thenReturn(List.of());
```

- [ ] **Step 5: Run all AuthService tests to verify they pass**

```bash
cd gestao-empresarial-backend && ./mvnw test -Dtest=AuthServiceImplTest
```
Expected: All 4 tests PASS (3 existing + 1 new).

- [ ] **Step 6: Run ALL backend tests**

```bash
cd gestao-empresarial-backend && ./mvnw test
```
Expected: All tests PASS (CnpjValidatorTest + CompanyServiceImplTest + AuthServiceImplTest).

- [ ] **Step 7: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/service/impl/AuthServiceImpl.java gestao-empresarial-backend/src/test/java/com/findash/service/impl/AuthServiceImplTest.java
git commit -m "feat(company): resolve pending invites on user registration"
```

---

## Chunk 4: Frontend

### Task 14: CompanyService + company guard

**Files:**
- Create: `gestao-empresaial-frontend/src/app/core/services/company.service.ts`
- Create: `gestao-empresaial-frontend/src/app/core/guards/company.guard.ts`

- [ ] **Step 1: Create CompanyService**

Follow same pattern as `auth.service.ts` — `inject()`, signals, `providedIn: 'root'`.

```typescript
// gestao-empresaial-frontend/src/app/core/services/company.service.ts
import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface CompanyResponse {
  id: string;
  name: string;
  cnpj: string | null;
  segment: string | null;
  ownerId: string;
  ownerName: string;
  role: string;
  active: boolean;
}

export interface CreateCompanyRequest {
  name: string;
  cnpj?: string;
  segment?: string;
}

export interface UpdateCompanyRequest {
  name: string;
  cnpj?: string;
  segment?: string;
}

export interface CompanyMemberResponse {
  userId: string | null;
  name: string | null;
  email: string;
  role: string | null;
  status: string;
  joinedAt: string | null;
}

export interface InviteMemberRequest {
  email: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly http = inject(HttpClient);

  private readonly _companies = signal<CompanyResponse[]>([]);
  private readonly _activeCompany = signal<CompanyResponse | null>(null);
  private readonly _loaded = signal(false);

  readonly companies = this._companies.asReadonly();
  readonly activeCompany = this._activeCompany.asReadonly();
  readonly isLoaded = this._loaded.asReadonly();
  readonly hasCompanies = computed(() => this._companies().length > 0);

  private readonly API_URL = '/api/companies';

  get activeCompanyId(): string | null {
    return this._activeCompany()?.id ?? null;
  }

  loadCompanies$(): Observable<CompanyResponse[]> {
    return this.http.get<CompanyResponse[]>(this.API_URL).pipe(
      tap((companies) => {
        this._companies.set(companies);
        this._loaded.set(true);
        this.restoreActiveCompany(companies);
      }),
    );
  }

  setActiveCompany(company: CompanyResponse) {
    this._activeCompany.set(company);
    localStorage.setItem('activeCompanyId', company.id);
  }

  addCompany(company: CompanyResponse) {
    this._companies.update((companies) => [...companies, company]);
  }

  createCompany(data: CreateCompanyRequest): Observable<CompanyResponse> {
    return this.http.post<CompanyResponse>(this.API_URL, data);
  }

  updateCompany(id: string, data: UpdateCompanyRequest): Observable<CompanyResponse> {
    return this.http.put<CompanyResponse>(`${this.API_URL}/${id}`, data);
  }

  getCompany(id: string): Observable<CompanyResponse> {
    return this.http.get<CompanyResponse>(`${this.API_URL}/${id}`);
  }

  getMembers(companyId: string): Observable<CompanyMemberResponse[]> {
    return this.http.get<CompanyMemberResponse[]>(`${this.API_URL}/${companyId}/members`);
  }

  inviteMember(companyId: string, data: InviteMemberRequest): Observable<CompanyMemberResponse> {
    return this.http.post<CompanyMemberResponse>(
      `${this.API_URL}/${companyId}/members/invite`,
      data,
    );
  }

  updateMemberRole(companyId: string, userId: string, role: string): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/${companyId}/members/${userId}/role`, { role });
  }

  removeMember(companyId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${companyId}/members/${userId}`);
  }

  reset() {
    this._companies.set([]);
    this._activeCompany.set(null);
    this._loaded.set(false);
    localStorage.removeItem('activeCompanyId');
  }

  private restoreActiveCompany(companies: CompanyResponse[]) {
    if (companies.length === 0) {
      this._activeCompany.set(null);
      return;
    }
    const savedId = localStorage.getItem('activeCompanyId');
    const saved = companies.find((c) => c.id === savedId);
    this._activeCompany.set(saved ?? companies[0]);
    if (!saved && companies.length > 0) {
      localStorage.setItem('activeCompanyId', companies[0].id);
    }
  }
}
```

- [ ] **Step 2: Create company guard**

```typescript
// gestao-empresaial-frontend/src/app/core/guards/company.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { CompanyService } from '../services/company.service';

export const companyGuard: CanActivateFn = () => {
  const companyService = inject(CompanyService);
  const router = inject(Router);

  if (companyService.isLoaded()) {
    if (companyService.hasCompanies()) {
      return true;
    }
    return router.createUrlTree(['/empresas/nova']);
  }

  return companyService.loadCompanies$().pipe(
    map((companies) => {
      if (companies.length === 0) {
        return router.createUrlTree(['/empresas/nova']);
      }
      return true;
    }),
  );
};
```

- [ ] **Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/core/services/company.service.ts gestao-empresaial-frontend/src/app/core/guards/company.guard.ts
git commit -m "feat(company): add CompanyService and companyGuard"
```

---

### Task 15: Auth interceptor update + routing

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/core/interceptors/auth.interceptor.ts`
- Modify: `gestao-empresaial-frontend/src/app/app.routes.ts`
- Create: `gestao-empresaial-frontend/src/app/features/company/company.routes.ts`

- [ ] **Step 1: Update auth interceptor to include X-Company-Id**

Replace the entire contents of `gestao-empresaial-frontend/src/app/core/interceptors/auth.interceptor.ts`:

```typescript
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { CompanyService } from '../services/company.service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const companyService = inject(CompanyService);
  const token = authService.accessToken;

  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const companyId = companyService.activeCompanyId;
  if (companyId) {
    headers['X-Company-Id'] = companyId;
  }

  const authReq = req.clone({ setHeaders: headers });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/')) {
        authService.logout();
      }
      return throwError(() => error);
    }),
  );
};
```

- [ ] **Step 2: Create company routes**

```typescript
// gestao-empresaial-frontend/src/app/features/company/company.routes.ts
import { Routes } from '@angular/router';

export const companyRoutes: Routes = [
  {
    path: 'configuracoes',
    loadComponent: () =>
      import('./pages/company-settings/company-settings.component').then(
        (m) => m.CompanySettingsComponent,
      ),
  },
  {
    path: 'configuracoes/usuarios',
    loadComponent: () =>
      import('./pages/user-management/user-management.component').then(
        (m) => m.UserManagementComponent,
      ),
  },
];
```

- [ ] **Step 3: Update app.routes.ts**

Replace the entire contents of `gestao-empresaial-frontend/src/app/app.routes.ts`:

```typescript
import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { authGuard } from './core/guards/auth.guard';
import { companyGuard } from './core/guards/company.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/pages/register/register.component').then(
        (m) => m.RegisterComponent,
      ),
  },
  {
    path: 'empresas/nova',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/company/pages/create-company/create-company.component').then(
        (m) => m.CreateCompanyComponent,
      ),
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard, companyGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((m) => m.dashboardRoutes),
      },
      {
        path: '',
        loadChildren: () =>
          import('./features/company/company.routes').then((m) => m.companyRoutes),
      },
    ],
  },
];
```

- [ ] **Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/core/interceptors/auth.interceptor.ts gestao-empresaial-frontend/src/app/features/company/company.routes.ts gestao-empresaial-frontend/src/app/app.routes.ts
git commit -m "feat(company): update interceptor with X-Company-Id, add company routes"
```

---

### Task 16: Create company page

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/company/pages/create-company/create-company.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/company/pages/create-company/create-company.component.html`

- [ ] **Step 1: Create component**

Follow same pattern as `login.component.ts` — `inject()`, signals, reactive form, `OnPush`.

```typescript
// create-company.component.ts
import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CompanyService } from '../../../../core/services/company.service';

@Component({
  selector: 'app-create-company',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './create-company.component.html',
})
export class CreateCompanyComponent {
  private readonly fb = inject(FormBuilder);
  private readonly companyService = inject(CompanyService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
    cnpj: [''],
    segment: [''],
  });

  protected submit() {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { name, cnpj, segment } = this.form.getRawValue();
    this.companyService
      .createCompany({
        name,
        cnpj: cnpj || undefined,
        segment: segment || undefined,
      })
      .subscribe({
        next: (company) => {
          this.companyService.addCompany(company);
          this.companyService.setActiveCompany(company);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err.error?.message || 'Erro ao criar empresa');
        },
      });
  }
}
```

- [ ] **Step 2: Create template**

Follow same visual pattern as `login.component.html` — centered card on gray background.

```html
<!-- create-company.component.html -->
<div class="min-h-screen flex items-center justify-center bg-gray-50">
  <div class="w-full max-w-md p-8 bg-white rounded-lg border border-gray-200">
    <h1 class="text-2xl font-bold text-gray-900 mb-2">Criar sua empresa</h1>
    <p class="text-sm text-gray-500 mb-6">
      Configure sua empresa para comecar a usar o FinDash
    </p>

    @if (error()) {
      <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
        {{ error() }}
      </div>
    }

    <form [formGroup]="form" (ngSubmit)="submit()">
      <div class="mb-4">
        <label class="block text-sm font-medium text-gray-700 mb-1">
          Nome da empresa <span class="text-red-500">*</span>
        </label>
        <input
          formControlName="name"
          type="text"
          class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          placeholder="Nome da sua empresa"
        />
      </div>

      <div class="mb-4">
        <label class="block text-sm font-medium text-gray-700 mb-1">CNPJ</label>
        <input
          formControlName="cnpj"
          type="text"
          class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          placeholder="00.000.000/0000-00 (opcional)"
        />
      </div>

      <div class="mb-6">
        <label class="block text-sm font-medium text-gray-700 mb-1">Segmento</label>
        <input
          formControlName="segment"
          type="text"
          class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          placeholder="Ex: Tecnologia, Varejo, Servicos (opcional)"
        />
      </div>

      <button
        type="submit"
        [disabled]="form.invalid || loading()"
        class="w-full py-2 px-4 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {{ loading() ? 'Criando...' : 'Criar empresa' }}
      </button>
    </form>
  </div>
</div>
```

- [ ] **Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/company/pages/create-company/
git commit -m "feat(company): add create company page"
```

---

### Task 17: Company settings page

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/company/pages/company-settings/company-settings.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/company/pages/company-settings/company-settings.component.html`

- [ ] **Step 1: Create component**

```typescript
// company-settings.component.ts
import { Component, ChangeDetectionStrategy, signal, inject, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CompanyService } from '../../../../core/services/company.service';

@Component({
  selector: 'app-company-settings',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './company-settings.component.html',
})
export class CompanySettingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly companyService = inject(CompanyService);

  protected readonly loading = signal(false);
  protected readonly success = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
    cnpj: [''],
    segment: [''],
  });

  ngOnInit() {
    const company = this.companyService.activeCompany();
    if (company) {
      this.form.patchValue({
        name: company.name,
        cnpj: company.cnpj ?? '',
        segment: company.segment ?? '',
      });
    }
  }

  protected save() {
    if (this.form.invalid) return;
    const company = this.companyService.activeCompany();
    if (!company) return;

    this.loading.set(true);
    this.error.set(null);
    this.success.set(false);

    const { name, cnpj, segment } = this.form.getRawValue();
    this.companyService
      .updateCompany(company.id, {
        name,
        cnpj: cnpj || undefined,
        segment: segment || undefined,
      })
      .subscribe({
        next: (updated) => {
          this.loading.set(false);
          this.success.set(true);
          this.companyService.setActiveCompany(updated);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err.error?.message || 'Erro ao atualizar empresa');
        },
      });
  }
}
```

- [ ] **Step 2: Create template**

```html
<!-- company-settings.component.html -->
<div class="max-w-2xl">
  <h1 class="text-2xl font-bold text-gray-900 mb-1">Configuracoes da empresa</h1>
  <p class="text-sm text-gray-500 mb-6">Atualize os dados da sua empresa</p>

  @if (success()) {
    <div class="mb-4 p-3 bg-emerald-50 border border-emerald-200 rounded-md text-sm text-emerald-600">
      Empresa atualizada com sucesso
    </div>
  }

  @if (error()) {
    <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
      {{ error() }}
    </div>
  }

  <form [formGroup]="form" (ngSubmit)="save()" class="bg-white border border-gray-200 rounded-lg p-6">
    <div class="mb-4">
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Nome da empresa <span class="text-red-500">*</span>
      </label>
      <input
        formControlName="name"
        type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
      />
    </div>

    <div class="mb-4">
      <label class="block text-sm font-medium text-gray-700 mb-1">CNPJ</label>
      <input
        formControlName="cnpj"
        type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        placeholder="00.000.000/0000-00"
      />
    </div>

    <div class="mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-1">Segmento</label>
      <input
        formControlName="segment"
        type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        placeholder="Ex: Tecnologia, Varejo, Servicos"
      />
    </div>

    <button
      type="submit"
      [disabled]="form.invalid || loading()"
      class="py-2 px-4 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
    >
      {{ loading() ? 'Salvando...' : 'Salvar alteracoes' }}
    </button>
  </form>
</div>
```

- [ ] **Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/company/pages/company-settings/
git commit -m "feat(company): add company settings page"
```

---

### Task 18: User management page

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/company/pages/user-management/user-management.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/company/pages/user-management/user-management.component.html`

- [ ] **Step 1: Create component**

```typescript
// user-management.component.ts
import { Component, ChangeDetectionStrategy, signal, inject, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import {
  CompanyService,
  CompanyMemberResponse,
} from '../../../../core/services/company.service';

@Component({
  selector: 'app-user-management',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-management.component.html',
})
export class UserManagementComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly companyService = inject(CompanyService);

  protected readonly members = signal<CompanyMemberResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly inviteLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly inviteSuccess = signal(false);

  protected readonly inviteForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['EDITOR', [Validators.required]],
  });

  ngOnInit() {
    this.loadMembers();
  }

  protected inviteMember() {
    if (this.inviteForm.invalid) return;
    const company = this.companyService.activeCompany();
    if (!company) return;

    this.inviteLoading.set(true);
    this.error.set(null);
    this.inviteSuccess.set(false);

    const { email, role } = this.inviteForm.getRawValue();
    this.companyService.inviteMember(company.id, { email, role }).subscribe({
      next: () => {
        this.inviteLoading.set(false);
        this.inviteSuccess.set(true);
        this.inviteForm.reset({ email: '', role: 'EDITOR' });
        this.loadMembers();
      },
      error: (err) => {
        this.inviteLoading.set(false);
        this.error.set(err.error?.message || 'Erro ao convidar membro');
      },
    });
  }

  protected changeRole(userId: string, newRole: string) {
    const company = this.companyService.activeCompany();
    if (!company) return;

    this.companyService.updateMemberRole(company.id, userId, newRole).subscribe({
      next: () => this.loadMembers(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao alterar role'),
    });
  }

  protected removeMember(userId: string) {
    const company = this.companyService.activeCompany();
    if (!company) return;

    this.companyService.removeMember(company.id, userId).subscribe({
      next: () => this.loadMembers(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao remover membro'),
    });
  }

  private loadMembers() {
    const company = this.companyService.activeCompany();
    if (!company) return;

    this.loading.set(true);
    this.companyService.getMembers(company.id).subscribe({
      next: (members) => {
        this.members.set(members);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
```

- [ ] **Step 2: Create template**

```html
<!-- user-management.component.html -->
<div class="max-w-4xl">
  <h1 class="text-2xl font-bold text-gray-900 mb-1">Gestao de usuarios</h1>
  <p class="text-sm text-gray-500 mb-6">Gerencie os membros da sua empresa</p>

  @if (error()) {
    <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
      {{ error() }}
    </div>
  }

  <!-- Invite form -->
  <div class="bg-white border border-gray-200 rounded-lg p-6 mb-6">
    <h2 class="text-lg font-semibold text-gray-900 mb-4">Convidar membro</h2>

    @if (inviteSuccess()) {
      <div class="mb-4 p-3 bg-emerald-50 border border-emerald-200 rounded-md text-sm text-emerald-600">
        Convite enviado com sucesso
      </div>
    }

    <form [formGroup]="inviteForm" (ngSubmit)="inviteMember()" class="flex gap-3 items-end">
      <div class="flex-1">
        <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
        <input
          formControlName="email"
          type="email"
          class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          placeholder="email@exemplo.com"
        />
      </div>
      <div class="w-40">
        <label class="block text-sm font-medium text-gray-700 mb-1">Role</label>
        <select
          formControlName="role"
          class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          <option value="EDITOR">Editor</option>
          <option value="VIEWER">Visualizador</option>
        </select>
      </div>
      <button
        type="submit"
        [disabled]="inviteForm.invalid || inviteLoading()"
        class="py-2 px-4 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {{ inviteLoading() ? 'Convidando...' : 'Convidar' }}
      </button>
    </form>
  </div>

  <!-- Members table -->
  <div class="bg-white border border-gray-200 rounded-lg overflow-hidden">
    <table class="w-full">
      <thead class="bg-gray-50 border-b border-gray-200">
        <tr>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Nome</th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Role</th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
          <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Acoes</th>
        </tr>
      </thead>
      <tbody>
        @if (loading()) {
          <tr>
            <td colspan="5" class="px-6 py-8 text-center text-sm text-gray-500">
              Carregando...
            </td>
          </tr>
        } @else {
          @for (member of members(); track member.email) {
            <tr class="border-b border-gray-100">
              <td class="px-6 py-4 text-sm text-gray-900">
                {{ member.name ?? '-' }}
              </td>
              <td class="px-6 py-4 text-sm text-gray-500">
                {{ member.email }}
              </td>
              <td class="px-6 py-4 text-sm">
                @if (member.role) {
                  <span
                    class="px-2 py-1 rounded-full text-xs font-medium"
                    [class]="
                      member.role === 'ADMIN'
                        ? 'bg-blue-50 text-blue-700'
                        : member.role === 'EDITOR'
                          ? 'bg-emerald-50 text-emerald-700'
                          : 'bg-gray-100 text-gray-600'
                    "
                  >
                    {{ member.role }}
                  </span>
                } @else {
                  <span class="text-gray-400">-</span>
                }
              </td>
              <td class="px-6 py-4 text-sm">
                <span
                  class="px-2 py-1 rounded-full text-xs font-medium"
                  [class]="
                    member.status === 'ACTIVE'
                      ? 'bg-emerald-50 text-emerald-700'
                      : 'bg-amber-50 text-amber-700'
                  "
                >
                  {{ member.status === 'ACTIVE' ? 'Ativo' : 'Convidado' }}
                </span>
              </td>
              <td class="px-6 py-4 text-right">
                @if (member.status === 'ACTIVE' && member.role !== 'ADMIN') {
                  <div class="flex gap-2 justify-end">
                    <select
                      class="px-2 py-1 border border-gray-200 rounded text-xs"
                      [value]="member.role"
                      (change)="changeRole(member.userId!, $any($event.target).value)"
                    >
                      <option value="EDITOR">Editor</option>
                      <option value="VIEWER">Visualizador</option>
                    </select>
                    <button
                      (click)="removeMember(member.userId!)"
                      class="px-2 py-1 text-xs text-red-600 hover:bg-red-50 rounded"
                    >
                      Remover
                    </button>
                  </div>
                }
              </td>
            </tr>
          }
        }
      </tbody>
    </table>
  </div>
</div>
```

- [ ] **Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/company/pages/user-management/
git commit -m "feat(company): add user management page"
```

---

### Task 19: Header company selector

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/layout/header/header.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/layout/header/header.component.html`

- [ ] **Step 1: Update header component**

Replace the entire contents of `header.component.ts`:

```typescript
import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CompanyService, CompanyResponse } from '../../core/services/company.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block h-16 bg-white border-b border-gray-200' },
  templateUrl: './header.component.html',
})
export class HeaderComponent {
  protected readonly companyService = inject(CompanyService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly dropdownOpen = signal(false);

  protected toggleDropdown() {
    this.dropdownOpen.update((v) => !v);
  }

  protected selectCompany(company: CompanyResponse) {
    this.companyService.setActiveCompany(company);
    this.dropdownOpen.set(false);
  }

  protected logout() {
    this.companyService.reset();
    this.authService.logout();
  }
}
```

- [ ] **Step 2: Update header template**

Replace the entire contents of `header.component.html`:

```html
<div class="flex items-center justify-between h-full px-6">
  <div></div>
  <div class="flex items-center gap-4">
    <!-- Company selector -->
    <div class="relative">
      <button
        (click)="toggleDropdown()"
        class="flex items-center gap-2 px-3 py-1.5 text-sm border border-gray-200 rounded-md hover:bg-gray-50"
      >
        <i class="pi pi-building text-gray-500"></i>
        <span class="text-gray-700">
          {{ companyService.activeCompany()?.name ?? 'Selecionar empresa' }}
        </span>
        <i class="pi pi-chevron-down text-xs text-gray-400"></i>
      </button>

      @if (dropdownOpen()) {
        <div
          class="absolute right-0 mt-1 w-64 bg-white border border-gray-200 rounded-md shadow-lg z-50"
        >
          @for (company of companyService.companies(); track company.id) {
            <button
              (click)="selectCompany(company)"
              class="w-full px-4 py-2.5 text-left text-sm hover:bg-gray-50 flex items-center justify-between"
              [class.bg-blue-50]="company.id === companyService.activeCompany()?.id"
            >
              <span class="text-gray-900">{{ company.name }}</span>
              @if (company.id === companyService.activeCompany()?.id) {
                <i class="pi pi-check text-blue-600 text-xs"></i>
              }
            </button>
          }
        </div>
      }
    </div>

    <!-- Logout -->
    <button
      (click)="logout()"
      class="flex items-center gap-2 px-3 py-1.5 text-sm rounded-md hover:bg-gray-50"
    >
      <i class="pi pi-sign-out text-gray-500"></i>
      <span class="text-gray-700">Sair</span>
    </button>
  </div>
</div>
```

- [ ] **Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/layout/header/
git commit -m "feat(company): add company selector dropdown in header"
```

---

### Task 20: Final verification

- [ ] **Step 1: Run all backend tests**

```bash
cd gestao-empresarial-backend && ./mvnw test
```
Expected: All tests PASS.

- [ ] **Step 2: Build frontend**

```bash
cd gestao-empresaial-frontend && npm run build
```
Expected: Build succeeds with no errors.

- [ ] **Step 3: Run frontend tests**

```bash
cd gestao-empresaial-frontend && npm test -- --run
```
Expected: Tests pass.

- [ ] **Step 4: Final commit (if any uncommitted changes)**

```bash
git status
```
If clean, no commit needed.

- [ ] **Step 5: Push branch**

```bash
git push -u origin feature/phase2-company-service
```
