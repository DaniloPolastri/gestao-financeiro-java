# Refatoracao de Estrutura Backend + Frontend — Implementation Plan

> **For Claude:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refatorar o backend de multi-module hexagonal para single-module com arquitetura em camadas (controller/service/repository/entity/dto/mapper), e reorganizar o frontend seguindo `docs/ESTRUTURA-FRONTEND.md`.

**Architecture:** Backend elimina multi-module Maven (shared, auth-service, api-gateway) e consolida tudo em um unico modulo com um `FindashApplication.java`. Classes do shared sao absorvidas no pacote principal `com.findash`. API Gateway e removido (frontend proxy aponta direto para o backend). Frontend reorganiza pastas: layout na raiz do app, features com `pages/`, templates separados.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, MapStruct 1.6.3, PostgreSQL, Flyway, Angular 21, Tailwind CSS 4

---

## File Structure — Backend (Single Module)

### Current (Multi-Module Hexagonal)
```
gestao-empresarial-backend/
├── pom.xml (parent, packaging=pom)
├── shared/       (findash-shared module)
│   └── com.findash.shared/ (BaseEntity, exceptions, security filters, JWT)
├── auth-service/ (auth-service module)
│   └── com.findash.auth/ (hexagonal: domain/port/adapter)
└── api-gateway/  (api-gateway module)
    └── com.findash.gateway/ (Spring Cloud Gateway)
```

### Target (Single Module Layered)
```
gestao-empresarial-backend/
├── pom.xml (single module, spring-boot-starter-parent)
└── src/main/java/com/findash/
    ├── FindashApplication.java
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── JwtConfig.java
    │   └── CorsConfig.java
    ├── controller/
    │   └── AuthController.java
    ├── service/
    │   ├── AuthService.java (interface)
    │   └── impl/
    │       └── AuthServiceImpl.java
    ├── repository/
    │   ├── UserRepository.java
    │   ├── UserRoleRepository.java
    │   └── RefreshTokenRepository.java
    ├── entity/
    │   ├── BaseEntity.java
    │   ├── User.java
    │   ├── UserRole.java
    │   ├── RefreshToken.java
    │   └── Role.java
    ├── dto/
    │   ├── RegisterRequestDTO.java
    │   ├── LoginRequestDTO.java
    │   ├── RefreshRequestDTO.java
    │   ├── AuthResponseDTO.java
    │   └── UserResponseDTO.java
    ├── mapper/
    │   └── AuthMapper.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── ApiErrorResponse.java
    │   ├── ResourceNotFoundException.java
    │   ├── BusinessRuleException.java
    │   └── DuplicateResourceException.java
    └── security/
        ├── JwtTokenProvider.java
        ├── JwtAuthenticationFilter.java
        ├── UserContext.java
        ├── CompanyContextHolder.java
        └── CompanyContextFilter.java
```

### What gets deleted entirely
- `shared/` module directory
- `auth-service/` module directory
- `api-gateway/` module directory
- Old parent `pom.xml` (replaced by single-module POM)

---

## File Structure — Frontend

### Current
```
src/app/
├── app.config.ts, app.routes.ts, app.ts, app.html, app.css
├── core/
│   ├── auth/guards/auth.guard.ts
│   ├── auth/interceptors/auth.interceptor.ts
│   ├── auth/services/auth.service.ts
│   └── layout/components/ (header/, layout/, sidebar/)
└── features/
    ├── auth/components/ (login/, register/)
    └── dashboard/components/dashboard/
```

### Target
```
src/app/
├── app.config.ts, app.routes.ts, app.component.ts, app.component.html, app.component.css
├── core/
│   ├── services/auth.service.ts
│   ├── guards/auth.guard.ts
│   ├── interceptors/auth.interceptor.ts
│   └── models/
├── layout/
│   ├── main-layout/ (.component.ts, .component.html)
│   ├── sidebar/ (.component.ts, .component.html)
│   └── header/ (.component.ts, .component.html)
├── features/
│   ├── auth/
│   │   ├── pages/ (login/, register/)
│   │   └── auth.routes.ts
│   └── dashboard/
│       ├── pages/dashboard/
│       └── dashboard.routes.ts
└── shared/
    ├── components/
    ├── pipes/
    └── directives/
```

---

## Chunk 1: Backend — New single-module structure

### Task 0: Create feature branch

- [ ] **Step 1: Create branch from current feature branch**

```bash
git checkout feature/phase1-infra-auth
git checkout -b feature/refactoring-structure
```

---

### Task 1: Create new single-module POM + application entry point

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/FindashApplication.java`
- Create: `gestao-empresarial-backend/src/main/resources/application.yml`
- Create: `gestao-empresarial-backend/src/main/resources/db/migration/V1__create_users_table.sql`
- Rewrite: `gestao-empresarial-backend/pom.xml`

- [ ] **Step 1: Rewrite pom.xml as single module**

Replace `gestao-empresarial-backend/pom.xml` entirely:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.2</version>
        <relativePath/>
    </parent>

    <groupId>com.findash</groupId>
    <artifactId>findash-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>FinDash Backend</name>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.6</jjwt.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <springdoc.version>2.8.5</springdoc.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-flyway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>

        <!-- API Docs -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Dev Tools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create FindashApplication.java**

Create `gestao-empresarial-backend/src/main/java/com/findash/FindashApplication.java`:

```java
package com.findash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FindashApplication {
    public static void main(String[] args) {
        SpringApplication.run(FindashApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.yml**

Create `gestao-empresarial-backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: findash-backend
  datasource:
    url: jdbc:postgresql://localhost:5433/findash?currentSchema=auth_schema
    username: findash
    password: findash_dev
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: auth_schema
        format_sql: true
    show-sql: false
  flyway:
    schemas: auth_schema
    locations: classpath:db/migration

server:
  port: 8080

app:
  jwt:
    secret: findash-dev-secret-key-that-is-at-least-32-bytes-long-for-hmac
    access-token-expiration-ms: 900000
    refresh-token-expiration-ms: 2592000000

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

Note: server port is now `8080` (single backend, no gateway).

- [ ] **Step 4: Copy Flyway migration**

Copy `auth-service/src/main/resources/db/migration/V1__create_users_table.sql` to `src/main/resources/db/migration/V1__create_users_table.sql` (same content).

- [ ] **Step 5: Commit**

```bash
git add gestao-empresarial-backend/pom.xml
git add gestao-empresarial-backend/src/main/java/com/findash/FindashApplication.java
git add gestao-empresarial-backend/src/main/resources/
git commit -m "refactor(backend): create single-module structure with FindashApplication"
```

---

### Task 2: Create entity, repository, exception, and security packages

**Files:**
- Create: `src/main/java/com/findash/entity/` (BaseEntity, User, UserRole, RefreshToken, Role)
- Create: `src/main/java/com/findash/repository/` (UserRepository, UserRoleRepository, RefreshTokenRepository)
- Create: `src/main/java/com/findash/exception/` (all exception classes from shared)
- Create: `src/main/java/com/findash/security/` (all security classes from shared)

All paths below relative to `gestao-empresarial-backend/src/main/java/com/findash/`.

- [ ] **Step 1: Create BaseEntity**

Create `entity/BaseEntity.java`:

```java
package com.findash.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 2: Create Role enum**

Create `entity/Role.java`:

```java
package com.findash.entity;

public enum Role {
    ADMIN, EDITOR, VIEWER
}
```

- [ ] **Step 3: Create User entity**

Create `entity/User.java`:

```java
package com.findash.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users", schema = "auth_schema")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean active = true;

    public User() {}

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.active = true;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
```

- [ ] **Step 4: Create UserRole entity**

Create `entity/UserRole.java`:

```java
package com.findash.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_roles", schema = "auth_schema",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "company_id"}))
public class UserRole extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
```

- [ ] **Step 5: Create RefreshToken entity**

Create `entity/RefreshToken.java`:

```java
package com.findash.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", schema = "auth_schema")
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isValid() { return !revoked && !isExpired(); }
}
```

- [ ] **Step 6: Create repositories**

Create `repository/UserRepository.java`:

```java
package com.findash.repository;

import com.findash.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Create `repository/UserRoleRepository.java`:

```java
package com.findash.repository;

import com.findash.entity.Role;
import com.findash.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserId(UUID userId);
    List<UserRole> findByCompanyId(UUID companyId);
    void deleteByUserIdAndCompanyId(UUID userId, UUID companyId);
    boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);
    long countByCompanyIdAndRole(UUID companyId, Role role);
}
```

Create `repository/RefreshTokenRepository.java`:

```java
package com.findash.repository;

import com.findash.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    void revokeAllByUserId(UUID userId);
}
```

- [ ] **Step 7: Create exception classes**

Create `exception/ApiErrorResponse.java`:

```java
package com.findash.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    int status,
    String message,
    List<FieldError> errors,
    Instant timestamp
) {
    public record FieldError(String field, String message) {}

    public static ApiErrorResponse of(int status, String message) {
        return new ApiErrorResponse(status, message, List.of(), Instant.now());
    }

    public static ApiErrorResponse of(int status, String message, List<FieldError> errors) {
        return new ApiErrorResponse(status, message, errors, Instant.now());
    }
}
```

Create `exception/ResourceNotFoundException.java`:

```java
package com.findash.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id);
    }
}
```

Create `exception/BusinessRuleException.java`:

```java
package com.findash.exception;

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
```

Create `exception/DuplicateResourceException.java`:

```java
package com.findash.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

Create `exception/GlobalExceptionHandler.java`:

```java
package com.findash.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(404, ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(409, ex.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of(422, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiErrorResponse.FieldError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> new ApiErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(400, "Validacao falhou", errors));
    }
}
```

- [ ] **Step 8: Create security classes**

Create `security/UserContext.java`:

```java
package com.findash.security;

import java.util.List;
import java.util.UUID;

public record UserContext(UUID userId, String email, List<String> roles) {}
```

Create `security/JwtTokenProvider.java` — same as current `shared/security/JwtTokenProvider.java` but with package `com.findash.security`.

Create `security/JwtAuthenticationFilter.java` — same as current but with package `com.findash.security`, importing `com.findash.security.UserContext` and `com.findash.security.JwtTokenProvider`.

Create `security/CompanyContextHolder.java` — same content, package `com.findash.security`.

Create `security/CompanyContextFilter.java` — same content, package `com.findash.security`.

- [ ] **Step 9: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/entity/
git add gestao-empresarial-backend/src/main/java/com/findash/repository/
git add gestao-empresarial-backend/src/main/java/com/findash/exception/
git add gestao-empresarial-backend/src/main/java/com/findash/security/
git commit -m "refactor(backend): add entity, repository, exception, and security packages"
```

---

## Chunk 2: Backend — DTO, Mapper, Service, Controller, Config

### Task 3: Create DTOs + Mapper

**Files:**
- Create: `src/main/java/com/findash/dto/` (5 DTO records)
- Create: `src/main/java/com/findash/mapper/AuthMapper.java`

- [ ] **Step 1: Create all DTOs**

Create `dto/RegisterRequestDTO.java`:

```java
package com.findash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
    @NotBlank(message = "Nome e obrigatorio") String name,
    @NotBlank(message = "Email e obrigatorio") @Email(message = "Email invalido") String email,
    @NotBlank(message = "Senha e obrigatoria") @Size(min = 6, message = "Senha deve ter no minimo 6 caracteres") String password
) {}
```

Create `dto/LoginRequestDTO.java`:

```java
package com.findash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank(message = "Email e obrigatorio") @Email(message = "Email invalido") String email,
    @NotBlank(message = "Senha e obrigatoria") String password
) {}
```

Create `dto/RefreshRequestDTO.java`:

```java
package com.findash.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDTO(
    @NotBlank(message = "Refresh token e obrigatorio") String refreshToken
) {}
```

Create `dto/UserResponseDTO.java`:

```java
package com.findash.dto;

import java.util.UUID;

public record UserResponseDTO(UUID id, String name, String email) {}
```

Create `dto/AuthResponseDTO.java`:

```java
package com.findash.dto;

public record AuthResponseDTO(String accessToken, String refreshToken, UserResponseDTO user) {}
```

- [ ] **Step 2: Create AuthMapper**

Create `mapper/AuthMapper.java`:

```java
package com.findash.mapper;

import com.findash.dto.AuthResponseDTO;
import com.findash.dto.UserResponseDTO;
import com.findash.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    UserResponseDTO toUserResponse(User user);

    default AuthResponseDTO toAuthResponse(String accessToken, String refreshToken, User user) {
        return new AuthResponseDTO(accessToken, refreshToken, toUserResponse(user));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/dto/
git add gestao-empresarial-backend/src/main/java/com/findash/mapper/
git commit -m "refactor(backend): add DTOs and AuthMapper"
```

---

### Task 4: Create Service interface + implementation

**Files:**
- Create: `src/main/java/com/findash/service/AuthService.java`
- Create: `src/main/java/com/findash/service/impl/AuthServiceImpl.java`

- [ ] **Step 1: Create AuthService interface**

Create `service/AuthService.java`:

```java
package com.findash.service;

import com.findash.dto.*;

public interface AuthService {
    AuthResponseDTO register(RegisterRequestDTO request);
    AuthResponseDTO login(LoginRequestDTO request);
    AuthResponseDTO refresh(RefreshRequestDTO request);
    void logout(RefreshRequestDTO request);
}
```

- [ ] **Step 2: Create AuthServiceImpl**

Create `service/impl/AuthServiceImpl.java`:

```java
package com.findash.service.impl;

import com.findash.dto.*;
import com.findash.entity.RefreshToken;
import com.findash.entity.User;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.AuthMapper;
import com.findash.repository.RefreshTokenRepository;
import com.findash.repository.UserRepository;
import com.findash.repository.UserRoleRepository;
import com.findash.security.JwtTokenProvider;
import com.findash.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthMapper mapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final long refreshTokenExpirationMs;

    public AuthServiceImpl(UserRepository userRepository,
                           UserRoleRepository userRoleRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           JwtTokenProvider tokenProvider,
                           AuthMapper mapper,
                           @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.mapper = mapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email ja cadastrado: " + request.email());
        }

        User user = new User(request.name(), request.email(),
                              passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        return createAuthResponse(user);
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Senha invalida");
        }

        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDTO refresh(RefreshRequestDTO request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", request.refreshToken()));

        if (!refreshToken.isValid()) {
            throw new IllegalArgumentException("Refresh token invalido ou expirado");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", refreshToken.getUserId()));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(RefreshRequestDTO request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private AuthResponseDTO createAuthResponse(User user) {
        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().name())
                .toList();

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshTokenStr = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshTokenRepository.save(refreshToken);

        return mapper.toAuthResponse(accessToken, refreshTokenStr, user);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/service/
git commit -m "refactor(backend): add AuthService interface + AuthServiceImpl"
```

---

### Task 5: Create Controller + Config

**Files:**
- Create: `src/main/java/com/findash/controller/AuthController.java`
- Create: `src/main/java/com/findash/config/SecurityConfig.java`
- Create: `src/main/java/com/findash/config/JwtConfig.java`
- Create: `src/main/java/com/findash/config/CorsConfig.java`

- [ ] **Step 1: Create AuthController**

Create `controller/AuthController.java`:

```java
package com.findash.controller;

import com.findash.dto.*;
import com.findash.security.UserContext;
import com.findash.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(service.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        return ResponseEntity.ok(service.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDTO request) {
        service.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(@AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(new UserResponseDTO(user.userId(), null, user.email()));
    }
}
```

- [ ] **Step 2: Create SecurityConfig**

Create `config/SecurityConfig.java`:

```java
package com.findash.config;

import com.findash.security.JwtAuthenticationFilter;
import com.findash.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;

    public SecurityConfig(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 3: Create JwtConfig**

Create `config/JwtConfig.java`:

```java
package com.findash.config;

import com.findash.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long expirationMs) {
        return new JwtTokenProvider(secret, expirationMs);
    }
}
```

- [ ] **Step 4: Create CorsConfig**

Create `config/CorsConfig.java`:

```java
package com.findash.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/controller/
git add gestao-empresarial-backend/src/main/java/com/findash/config/
git commit -m "refactor(backend): add AuthController and config classes"
```

---

### Task 6: Create tests + delete old modules

**Files:**
- Create: `src/test/java/com/findash/service/impl/AuthServiceImplTest.java`
- Delete: `shared/` directory
- Delete: `auth-service/` directory
- Delete: `api-gateway/` directory

- [ ] **Step 1: Create AuthServiceImplTest**

Create `gestao-empresarial-backend/src/test/java/com/findash/service/impl/AuthServiceImplTest.java`:

```java
package com.findash.service.impl;

import com.findash.dto.*;
import com.findash.entity.Role;
import com.findash.entity.User;
import com.findash.entity.UserRole;
import com.findash.exception.DuplicateResourceException;
import com.findash.exception.ResourceNotFoundException;
import com.findash.mapper.AuthMapper;
import com.findash.repository.RefreshTokenRepository;
import com.findash.repository.UserRepository;
import com.findash.repository.UserRoleRepository;
import com.findash.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthMapper mapper;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, userRoleRepository,
                refreshTokenRepository, tokenProvider, mapper, 2592000000L);
    }

    @Test
    void login_withInvalidEmail_throwsException() {
        when(userRepository.findByEmail("bad@email.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> authService.login(new LoginRequestDTO("bad@email.com", "password")));
    }

    @Test
    void register_withExistingEmail_throwsDuplicateException() {
        when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> authService.register(new RegisterRequestDTO("Test", "existing@email.com", "password")));
    }

    @Test
    void register_withNewEmail_createsUser() {
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
                        new UserResponseDTO(UUID.randomUUID(), "Test", "new@email.com")));

        AuthResponseDTO result = authService.register(
                new RegisterRequestDTO("Test", "new@email.com", "password"));

        assertNotNull(result);
        assertEquals("new@email.com", result.user().email());
        verify(userRepository).save(any());
    }
}
```

- [ ] **Step 2: Delete old module directories**

```bash
rm -rf gestao-empresarial-backend/shared
rm -rf gestao-empresarial-backend/auth-service
rm -rf gestao-empresarial-backend/api-gateway
```

- [ ] **Step 3: Verify build compiles**

```bash
cd gestao-empresarial-backend && ./mvnw clean compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Run tests**

```bash
cd gestao-empresarial-backend && ./mvnw test
```

Expected: All 3 tests pass.

- [ ] **Step 5: Verify app starts**

```bash
docker-compose up -d postgres
cd gestao-empresarial-backend && ./mvnw spring-boot:run
```

Expected: App starts on port 8080. Test:

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"refactor@test.com","password":"123456"}'
```

Expected: 201 Created with JWT response.

- [ ] **Step 6: Update frontend proxy to port 8080**

Update `gestao-empresaial-frontend/proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

(Already points to 8080 — just verify.)

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(backend): delete old modules, complete single-module migration"
```

---

## Chunk 3: Frontend — Layout + Core reorganization

### Task 7: Move layout to app-level + separate templates

**Files:**
- Create: `src/app/layout/sidebar/sidebar.component.ts` + `.html`
- Create: `src/app/layout/header/header.component.ts` + `.html`
- Create: `src/app/layout/main-layout/main-layout.component.ts` + `.html`

All paths below relative to `gestao-empresaial-frontend/`.

- [ ] **Step 1: Create sidebar files**

Create `src/app/layout/sidebar/sidebar.component.html`:

```html
<div class="p-6">
  <h1 class="text-xl font-bold text-gray-900">FinDash</h1>
</div>
<nav class="px-3">
  @for (item of mainNav; track item.route) {
    <a
      [routerLink]="item.route"
      routerLinkActive="bg-blue-50 text-blue-700 border-blue-500"
      class="flex items-center gap-3 px-3 py-2 rounded-md text-sm text-gray-700 hover:bg-gray-100 mb-1"
    >
      <i [class]="item.icon + ' text-base'"></i>
      {{ item.label }}
    </a>
  }
  <div class="border-t border-gray-200 my-4"></div>
  @for (item of settingsNav; track item.route) {
    <a
      [routerLink]="item.route"
      routerLinkActive="bg-blue-50 text-blue-700 border-blue-500"
      class="flex items-center gap-3 px-3 py-2 rounded-md text-sm text-gray-700 hover:bg-gray-100 mb-1"
    >
      <i [class]="item.icon + ' text-base'"></i>
      {{ item.label }}
    </a>
  }
</nav>
```

Create `src/app/layout/sidebar/sidebar.component.ts`:

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem { label: string; icon: string; route: string; }

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block w-64 min-h-screen bg-gray-50 border-r border-gray-200' },
  templateUrl: './sidebar.component.html',
})
export class SidebarComponent {
  protected readonly mainNav: NavItem[] = [
    { label: 'Dashboard', icon: 'pi pi-chart-bar', route: '/dashboard' },
    { label: 'Contas a Pagar', icon: 'pi pi-arrow-up-right', route: '/contas-a-pagar' },
    { label: 'Contas a Receber', icon: 'pi pi-arrow-down-left', route: '/contas-a-receber' },
    { label: 'Importacao', icon: 'pi pi-upload', route: '/importacao' },
    { label: 'Categorias', icon: 'pi pi-tags', route: '/categorias' },
    { label: 'Fornecedores', icon: 'pi pi-building', route: '/fornecedores' },
    { label: 'Clientes', icon: 'pi pi-users', route: '/clientes' },
  ];

  protected readonly settingsNav: NavItem[] = [
    { label: 'Empresa', icon: 'pi pi-cog', route: '/configuracoes' },
    { label: 'Usuarios', icon: 'pi pi-user-edit', route: '/configuracoes/usuarios' },
  ];
}
```

- [ ] **Step 2: Create header files**

Create `src/app/layout/header/header.component.html` — same content as current inline template.

Create `src/app/layout/header/header.component.ts`:

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-header',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block h-16 bg-white border-b border-gray-200' },
  templateUrl: './header.component.html',
})
export class HeaderComponent {}
```

- [ ] **Step 3: Create main-layout files**

Create `src/app/layout/main-layout/main-layout.component.html` — same content as current inline template.

Create `src/app/layout/main-layout/main-layout.component.ts`:

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, SidebarComponent, HeaderComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './main-layout.component.html',
})
export class MainLayoutComponent {}
```

- [ ] **Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/layout/
git commit -m "refactor(frontend): create layout/ at app level with separated templates"
```

---

### Task 8: Reorganize core/ + feature pages + routes + cleanup

**Files:**
- Create: `core/services/auth.service.ts` (move from core/auth/services/)
- Create: `core/guards/auth.guard.ts` (move from core/auth/guards/)
- Create: `core/interceptors/auth.interceptor.ts` (move from core/auth/interceptors/)
- Create: `features/auth/pages/login/login.component.ts` + `.html`
- Create: `features/auth/pages/register/register.component.ts` + `.html`
- Create: `features/auth/auth.routes.ts`
- Create: `features/dashboard/pages/dashboard/dashboard.component.ts` + `.html`
- Create: `features/dashboard/dashboard.routes.ts`
- Rename: `app.ts` → `app.component.ts`, `app.html` → `app.component.html`, `app.css` → `app.component.css`
- Update: `app.routes.ts`, `app.config.ts`, `main.ts`
- Delete: old `core/auth/`, old `core/layout/`, old `features/*/components/`
- Create: `shared/` skeleton + `core/models/` skeleton

- [ ] **Step 1: Create new core files**

Create `core/services/auth.service.ts` — same content as current, no path changes needed (providedIn: 'root').

Create `core/guards/auth.guard.ts` — update import to `../services/auth.service`.

Create `core/interceptors/auth.interceptor.ts` — update import to `../services/auth.service`.

- [ ] **Step 2: Create feature auth pages**

For each page (login, register): extract inline template to `.component.html`, update `.component.ts` to use `templateUrl`, update auth service import path to `../../../../core/services/auth.service`.

Create `features/auth/auth.routes.ts`:

```typescript
import { Routes } from '@angular/router';

export const authRoutes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/register/register.component').then((m) => m.RegisterComponent),
  },
];
```

Create `features/dashboard/dashboard.routes.ts`:

```typescript
import { Routes } from '@angular/router';

export const dashboardRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
];
```

- [ ] **Step 3: Rename app root files**

Rename `app.ts` → `app.component.ts`, update class name to `AppComponent`, use `templateUrl: './app.component.html'`, `styleUrl: './app.component.css'`.

Rename `app.html` → `app.component.html`.

Rename `app.css` → `app.component.css`.

Update `main.ts`: change import from `./app/app` to `./app/app.component` if needed.

- [ ] **Step 4: Update app.routes.ts**

```typescript
import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((m) => m.dashboardRoutes),
      },
    ],
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/pages/register/register.component').then((m) => m.RegisterComponent),
  },
];
```

- [ ] **Step 5: Update app.config.ts**

Update import path: `./core/interceptors/auth.interceptor`.

- [ ] **Step 6: Delete old directories**

```bash
rm -rf gestao-empresaial-frontend/src/app/core/auth
rm -rf gestao-empresaial-frontend/src/app/core/layout
rm -rf gestao-empresaial-frontend/src/app/features/auth/components
rm -rf gestao-empresaial-frontend/src/app/features/dashboard/components
rm -f gestao-empresaial-frontend/src/app/app.ts
rm -f gestao-empresaial-frontend/src/app/app.html
rm -f gestao-empresaial-frontend/src/app/app.css
```

- [ ] **Step 7: Create shared/ and core/models/ skeletons**

```bash
mkdir -p gestao-empresaial-frontend/src/app/shared/components
mkdir -p gestao-empresaial-frontend/src/app/shared/pipes
mkdir -p gestao-empresaial-frontend/src/app/shared/directives
mkdir -p gestao-empresaial-frontend/src/app/core/models
```

Add `.gitkeep` to empty directories.

- [ ] **Step 8: Verify frontend builds**

```bash
cd gestao-empresaial-frontend && npx ng build
```

Expected: Build succeeds.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor(frontend): reorganize to feature-based structure with separated templates"
```

---

## Chunk 4: Verification + PR

### Task 9: End-to-end verification

- [ ] **Step 1: Run backend tests**

```bash
cd gestao-empresarial-backend && ./mvnw test
```

Expected: All tests pass.

- [ ] **Step 2: Start backend and test endpoints**

```bash
docker-compose up -d postgres
cd gestao-empresarial-backend && ./mvnw spring-boot:run
```

Test register:
```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Final Test","email":"final@test.com","password":"123456"}'
```

Test login:
```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"final@test.com","password":"123456"}'
```

Expected: Both return JWT responses.

- [ ] **Step 3: Verify frontend builds**

```bash
cd gestao-empresaial-frontend && npx ng build
```

Expected: Clean build.

- [ ] **Step 4: Commit if needed**

```bash
git add -A
git commit -m "refactor: complete single-module + layered architecture migration"
```

---

### Task 10: Update docs + Push + PR

- [ ] **Step 1: Update MEMORY.md**

Update architecture section to reflect:
- Single module (no multi-module, no gateway)
- Layered: controller/service/repository/entity/dto/mapper
- Package base: `com.findash` (no sub-packages per service)
- Backend runs on port 8080 directly

- [ ] **Step 2: Push and create PR**

```bash
git push -u origin feature/refactoring-structure
gh pr create --title "Refactor: single-module layered architecture + frontend reorganization" --body "$(cat <<'EOF'
## Summary
- Backend: removed multi-module Maven (shared, auth-service, api-gateway) → single module
- Backend: migrated from hexagonal to layered architecture (controller/service/repository/entity/dto/mapper)
- Backend: single FindashApplication on port 8080 (no API Gateway)
- Frontend: layout at app level, features with pages/, separated HTML templates
- All existing auth functionality preserved

## Test plan
- [ ] Backend tests pass: `./mvnw test`
- [ ] Backend starts on :8080
- [ ] Register + Login work via curl
- [ ] Swagger UI at /swagger-ui.html
- [ ] Frontend builds: `npm run build`
- [ ] Layout renders (sidebar + header + content)
- [ ] Login/Register pages work

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Summary

| Chunk | Tasks | Description |
|-------|-------|-------------|
| 1 | 0-2 | Backend: Branch + single POM + entities/repos/exceptions/security |
| 2 | 3-6 | Backend: DTOs, mapper, service, controller, config, delete old modules |
| 3 | 7-8 | Frontend: Layout + core/features reorganization |
| 4 | 9-10 | E2E verification + PR |

**Key changes:**
- **Backend**: Multi-module hexagonal → Single-module layered. Removes ~50 files across 3 modules. Creates ~25 files in flat `com.findash.*` packages. Gateway removed entirely.
- **Frontend**: Reorganizes ~15 files. Layout at app level. Features use `pages/`. Templates separated. `.component.ts` naming.
