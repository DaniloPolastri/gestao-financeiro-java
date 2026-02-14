# Phase 1: Infrastructure + Auth Service — Implementation Plan

> **For Claude:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up the microservices infrastructure (Docker, Maven modules, Gateway) and implement a fully functional Auth Service with JWT authentication, plus the Angular frontend shell with login/register pages.

**Architecture:** Multi-module Maven project with hexagonal architecture per service. Spring Cloud Gateway routes requests. Auth Service handles users, JWT, and RBAC. Frontend has layout shell with sidebar/header and auth pages.

**Tech Stack:** Java 21, Spring Boot 4.0.2, Spring Cloud Gateway MVC, Spring Security, jjwt, Flyway, PostgreSQL 17, MapStruct, SpringDoc OpenAPI, Angular 21, Tailwind CSS 4, PrimeNG, Vitest.

**Design Spec:** `docs/superpowers/specs/2026-02-14-findash-mvp-design.md`

---

## Chunk 1: Infrastructure Setup

### Task 1: Docker Compose + PostgreSQL

**Files:**
- Create: `docker-compose.yml`
- Create: `init-schemas.sql`

- [ ] **Step 1: Create init-schemas.sql**

```sql
-- init-schemas.sql
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS company_schema;
CREATE SCHEMA IF NOT EXISTS financial_schema;
```

- [ ] **Step 2: Create docker-compose.yml**

```yaml
services:
  postgres:
    image: postgres:17
    container_name: findash-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: findash
      POSTGRES_USER: findash
      POSTGRES_PASSWORD: findash_dev
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-schemas.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U findash"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

Note: Only PostgreSQL for now. Java services run natively with `mvnw` for hot-reload during dev. Dockerized services will be added when deploying.

- [ ] **Step 3: Verify PostgreSQL starts**

Run: `docker-compose up -d postgres`
Then: `docker exec findash-postgres psql -U findash -d findash -c "\dn"`
Expected: Lists auth_schema, company_schema, financial_schema, public.

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml init-schemas.sql
git commit -m "infra: add Docker Compose with PostgreSQL and schema init"
```

---

### Task 2: Restructure Backend to Multi-Module Maven

**Context:** Current backend is a single Spring Boot app at `gestao-empresarial-backend/`. We need to transform it into a parent POM with child modules. The current main class and test will be removed and replaced by module-specific ones.

**Files:**
- Modify: `gestao-empresarial-backend/pom.xml` (becomes parent POM)
- Delete: `gestao-empresarial-backend/src/` (replaced by modules)
- Create: `gestao-empresarial-backend/shared/pom.xml`
- Create: `gestao-empresarial-backend/auth-service/pom.xml`
- Create: `gestao-empresarial-backend/api-gateway/pom.xml`

- [ ] **Step 1: Create parent POM**

Replace `gestao-empresarial-backend/pom.xml` with:

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
    <artifactId>findash-parent</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>FinDash Parent</name>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2024.0.1</spring-cloud.version>
        <jjwt.version>0.12.6</jjwt.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <springdoc.version>2.8.5</springdoc.version>
    </properties>

    <modules>
        <module>shared</module>
        <module>auth-service</module>
        <module>api-gateway</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.findash</groupId>
                <artifactId>findash-shared</artifactId>
                <version>${project.version}</version>
            </dependency>
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
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 2: Delete old src/ directory**

```bash
rm -rf gestao-empresarial-backend/src
```

- [ ] **Step 3: Create shared module pom.xml**

Create `gestao-empresarial-backend/shared/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.findash</groupId>
        <artifactId>findash-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>findash-shared</artifactId>
    <name>FinDash Shared</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.persistence</groupId>
            <artifactId>jakarta.persistence-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Create auth-service pom.xml**

Create `gestao-empresarial-backend/auth-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.findash</groupId>
        <artifactId>findash-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>auth-service</artifactId>
    <name>FinDash Auth Service</name>

    <dependencies>
        <dependency>
            <groupId>com.findash</groupId>
            <artifactId>findash-shared</artifactId>
        </dependency>
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
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
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
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
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

- [ ] **Step 5: Create api-gateway pom.xml**

Create `gestao-empresarial-backend/api-gateway/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.findash</groupId>
        <artifactId>findash-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>api-gateway</artifactId>
    <name>FinDash API Gateway</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway-mvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 6: Verify multi-module compiles**

Run from `gestao-empresarial-backend/`:
```bash
./mvnw clean compile -pl shared
```
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add -A gestao-empresarial-backend/
git commit -m "infra: restructure backend to multi-module Maven (shared, auth-service, api-gateway)"
```

---

### Task 3: Shared Module — Core Classes

**Files:**
- Create: `shared/src/main/java/com/findash/shared/` (multiple files)

- [ ] **Step 1: Create BaseEntity**

Create `shared/src/main/java/com/findash/shared/persistence/BaseEntity.java`:

```java
package com.findash.shared.persistence;

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

- [ ] **Step 2: Create ApiErrorResponse**

Create `shared/src/main/java/com/findash/shared/exception/ApiErrorResponse.java`:

```java
package com.findash.shared.exception;

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

- [ ] **Step 3: Create custom exceptions**

Create `shared/src/main/java/com/findash/shared/exception/ResourceNotFoundException.java`:

```java
package com.findash.shared.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id);
    }
}
```

Create `shared/src/main/java/com/findash/shared/exception/BusinessRuleException.java`:

```java
package com.findash.shared.exception;

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
```

Create `shared/src/main/java/com/findash/shared/exception/DuplicateResourceException.java`:

```java
package com.findash.shared.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

- [ ] **Step 4: Create GlobalExceptionHandler**

Create `shared/src/main/java/com/findash/shared/exception/GlobalExceptionHandler.java`:

```java
package com.findash.shared.exception;

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

- [ ] **Step 5: Create UserContext**

Create `shared/src/main/java/com/findash/shared/security/UserContext.java`:

```java
package com.findash.shared.security;

import java.util.List;
import java.util.UUID;

public record UserContext(
    UUID userId,
    String email,
    List<String> roles
) {}
```

- [ ] **Step 6: Create JwtTokenProvider**

Create `shared/src/main/java/com/findash/shared/security/JwtTokenProvider.java`:

```java
package com.findash.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMs;

    public JwtTokenProvider(String secret, long accessTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(key)
                .compact();
    }

    public UserContext parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);

        return new UserContext(userId, email, roles);
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
```

- [ ] **Step 7: Create JwtAuthenticationFilter**

Create `shared/src/main/java/com/findash/shared/security/JwtAuthenticationFilter.java`:

```java
package com.findash.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (tokenProvider.isValid(token)) {
                UserContext user = tokenProvider.parseToken(token);
                var authorities = user.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 8: Create CompanyContextHolder**

Create `shared/src/main/java/com/findash/shared/security/CompanyContextHolder.java`:

```java
package com.findash.shared.security;

import java.util.UUID;

public final class CompanyContextHolder {

    private static final ThreadLocal<UUID> COMPANY_ID = new ThreadLocal<>();

    private CompanyContextHolder() {}

    public static void set(UUID companyId) { COMPANY_ID.set(companyId); }
    public static UUID get() { return COMPANY_ID.get(); }
    public static void clear() { COMPANY_ID.remove(); }
}
```

Create `shared/src/main/java/com/findash/shared/security/CompanyContextFilter.java`:

```java
package com.findash.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class CompanyContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String companyIdHeader = request.getHeader("X-Company-Id");
            if (companyIdHeader != null && !companyIdHeader.isBlank()) {
                CompanyContextHolder.set(UUID.fromString(companyIdHeader));
            }
            filterChain.doFilter(request, response);
        } finally {
            CompanyContextHolder.clear();
        }
    }
}
```

- [ ] **Step 9: Verify shared module compiles**

Run from `gestao-empresarial-backend/`:
```bash
./mvnw clean compile -pl shared
```
Expected: BUILD SUCCESS

- [ ] **Step 10: Commit**

```bash
git add gestao-empresarial-backend/shared/
git commit -m "feat(shared): add base entity, exception handling, JWT, and company context filters"
```

---

## Chunk 2: Auth Service Backend

### Task 4: Auth Service — Domain Layer

**Files:**
- Create: `auth-service/src/main/java/com/findash/auth/` (domain package)

- [ ] **Step 1: Create domain models**

Create `auth-service/src/main/java/com/findash/auth/domain/model/User.java`:

```java
package com.findash.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public class User {
    private UUID id;
    private String name;
    private String email;
    private String password;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public User(UUID id, String name, String email, String password, boolean active,
                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

Create `auth-service/src/main/java/com/findash/auth/domain/model/Role.java`:

```java
package com.findash.auth.domain.model;

public enum Role {
    ADMIN, EDITOR, VIEWER
}
```

Create `auth-service/src/main/java/com/findash/auth/domain/model/UserRole.java`:

```java
package com.findash.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public class UserRole {
    private UUID id;
    private UUID userId;
    private UUID companyId;
    private Role role;
    private Instant createdAt;

    public UserRole(UUID id, UUID userId, UUID companyId, Role role, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.companyId = companyId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCompanyId() { return companyId; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setRole(Role role) { this.role = role; }
}
```

Create `auth-service/src/main/java/com/findash/auth/domain/model/RefreshToken.java`:

```java
package com.findash.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String token;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt;

    public RefreshToken(UUID id, UUID userId, String token, Instant expiresAt,
                        boolean revoked, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isValid() { return !revoked && !isExpired(); }
}
```

- [ ] **Step 2: Create output ports (repository interfaces)**

Create `auth-service/src/main/java/com/findash/auth/domain/port/out/UserRepositoryPort.java`:

```java
package com.findash.auth.domain.port.out;

import com.findash.auth.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Create `auth-service/src/main/java/com/findash/auth/domain/port/out/UserRoleRepositoryPort.java`:

```java
package com.findash.auth.domain.port.out;

import com.findash.auth.domain.model.UserRole;
import java.util.List;
import java.util.UUID;

public interface UserRoleRepositoryPort {
    UserRole save(UserRole userRole);
    List<UserRole> findByUserId(UUID userId);
    List<UserRole> findByCompanyId(UUID companyId);
    void deleteByUserIdAndCompanyId(UUID userId, UUID companyId);
    boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);
    long countByCompanyIdAndRole(UUID companyId, com.findash.auth.domain.model.Role role);
}
```

Create `auth-service/src/main/java/com/findash/auth/domain/port/out/RefreshTokenRepositoryPort.java`:

```java
package com.findash.auth.domain.port.out;

import com.findash.auth.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByToken(String token);
    void revokeAllByUserId(UUID userId);
}
```

Create `auth-service/src/main/java/com/findash/auth/domain/port/out/PasswordEncoderPort.java`:

```java
package com.findash.auth.domain.port.out;

public interface PasswordEncoderPort {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
```

- [ ] **Step 3: Create input ports (use case interfaces)**

Create `auth-service/src/main/java/com/findash/auth/domain/port/in/RegisterUserUseCase.java`:

```java
package com.findash.auth.domain.port.in;

import com.findash.auth.domain.model.User;

public interface RegisterUserUseCase {
    User register(String name, String email, String password);
}
```

Create `auth-service/src/main/java/com/findash/auth/domain/port/in/AuthenticateUseCase.java`:

```java
package com.findash.auth.domain.port.in;

public interface AuthenticateUseCase {
    record AuthResult(String accessToken, String refreshToken, java.util.UUID userId,
                      String name, String email) {}
    AuthResult authenticate(String email, String password);
    AuthResult refreshToken(String refreshToken);
    void logout(String refreshToken);
}
```

Create `auth-service/src/main/java/com/findash/auth/domain/port/in/ManageUserRolesUseCase.java`:

```java
package com.findash.auth.domain.port.in;

import com.findash.auth.domain.model.Role;
import com.findash.auth.domain.model.UserRole;
import java.util.List;
import java.util.UUID;

public interface ManageUserRolesUseCase {
    UserRole assignRole(UUID userId, UUID companyId, Role role);
    void updateRole(UUID userId, UUID companyId, Role newRole);
    void removeFromCompany(UUID userId, UUID companyId);
    List<UserRole> getUserRolesForCompany(UUID companyId);
}
```

Create `auth-service/src/main/java/com/findash/auth/domain/port/in/GetUserUseCase.java`:

```java
package com.findash.auth.domain.port.in;

import com.findash.auth.domain.model.User;
import java.util.UUID;

public interface GetUserUseCase {
    User getById(UUID id);
    User getByEmail(String email);
    User updateProfile(UUID id, String name);
}
```

- [ ] **Step 4: Write tests for AuthService domain**

Create `auth-service/src/test/java/com/findash/auth/domain/service/AuthServiceTest.java`:

```java
package com.findash.auth.domain.service;

import com.findash.auth.domain.model.RefreshToken;
import com.findash.auth.domain.model.Role;
import com.findash.auth.domain.model.User;
import com.findash.auth.domain.model.UserRole;
import com.findash.auth.domain.port.in.AuthenticateUseCase.AuthResult;
import com.findash.auth.domain.port.out.*;
import com.findash.shared.exception.ResourceNotFoundException;
import com.findash.shared.security.JwtTokenProvider;
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
class AuthServiceTest {

    @Mock private UserRepositoryPort userRepo;
    @Mock private UserRoleRepositoryPort userRoleRepo;
    @Mock private RefreshTokenRepositoryPort refreshTokenRepo;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepo, userRoleRepo, refreshTokenRepo,
                                       passwordEncoder, tokenProvider, 2592000000L);
    }

    @Test
    void authenticate_withValidCredentials_returnsAuthResult() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Test", "test@email.com", "encoded", true,
                             Instant.now(), Instant.now());
        UserRole role = new UserRole(UUID.randomUUID(), userId, UUID.randomUUID(),
                                     Role.ADMIN, Instant.now());

        when(userRepo.findByEmail("test@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(userRoleRepo.findByUserId(userId)).thenReturn(List.of(role));
        when(tokenProvider.generateAccessToken(eq(userId), eq("test@email.com"), anyList()))
                .thenReturn("jwt-token");
        when(refreshTokenRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResult result = authService.authenticate("test@email.com", "password");

        assertNotNull(result);
        assertEquals("jwt-token", result.accessToken());
        assertNotNull(result.refreshToken());
        assertEquals(userId, result.userId());
    }

    @Test
    void authenticate_withInvalidEmail_throwsException() {
        when(userRepo.findByEmail("bad@email.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> authService.authenticate("bad@email.com", "password"));
    }

    @Test
    void authenticate_withWrongPassword_throwsException() {
        User user = new User(UUID.randomUUID(), "Test", "test@email.com", "encoded",
                             true, Instant.now(), Instant.now());
        when(userRepo.findByEmail("test@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.authenticate("test@email.com", "wrong"));
    }
}
```

- [ ] **Step 5: Run test to verify it fails**

```bash
cd gestao-empresarial-backend && ./mvnw test -pl auth-service -Dtest=AuthServiceTest -Dsurefire.failIfNoTests=false
```
Expected: FAIL — `AuthService` class does not exist yet.

- [ ] **Step 6: Implement AuthService**

Create `auth-service/src/main/java/com/findash/auth/domain/service/AuthService.java`:

```java
package com.findash.auth.domain.service;

import com.findash.auth.domain.model.RefreshToken;
import com.findash.auth.domain.model.User;
import com.findash.auth.domain.model.UserRole;
import com.findash.auth.domain.port.in.AuthenticateUseCase;
import com.findash.auth.domain.port.out.*;
import com.findash.shared.exception.ResourceNotFoundException;
import com.findash.shared.security.JwtTokenProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AuthService implements AuthenticateUseCase {

    private final UserRepositoryPort userRepo;
    private final UserRoleRepositoryPort userRoleRepo;
    private final RefreshTokenRepositoryPort refreshTokenRepo;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long refreshTokenExpirationMs;

    public AuthService(UserRepositoryPort userRepo, UserRoleRepositoryPort userRoleRepo,
                       RefreshTokenRepositoryPort refreshTokenRepo,
                       PasswordEncoderPort passwordEncoder, JwtTokenProvider tokenProvider,
                       long refreshTokenExpirationMs) {
        this.userRepo = userRepo;
        this.userRoleRepo = userRoleRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Override
    public AuthResult authenticate(String email, String password) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Senha invalida");
        }

        List<String> roles = userRoleRepo.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().name())
                .toList();

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);
        String refreshTokenStr = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken(
                null, user.getId(), refreshTokenStr,
                Instant.now().plusMillis(refreshTokenExpirationMs),
                false, Instant.now()
        );
        refreshTokenRepo.save(refreshToken);

        return new AuthResult(accessToken, refreshTokenStr, user.getId(),
                              user.getName(), user.getEmail());
    }

    @Override
    public AuthResult refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(refreshTokenStr)
                .orElseThrow(() -> new ResourceNotFoundException("RefreshToken", refreshTokenStr));

        if (!refreshToken.isValid()) {
            throw new IllegalArgumentException("Refresh token invalido ou expirado");
        }

        User user = userRepo.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", refreshToken.getUserId()));

        List<String> roles = userRoleRepo.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().name())
                .toList();

        // Revoke old, create new
        refreshToken.setRevoked(true);
        refreshTokenRepo.save(refreshToken);

        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);
        String newRefreshTokenStr = UUID.randomUUID().toString();

        RefreshToken newRefreshToken = new RefreshToken(
                null, user.getId(), newRefreshTokenStr,
                Instant.now().plusMillis(refreshTokenExpirationMs),
                false, Instant.now()
        );
        refreshTokenRepo.save(newRefreshToken);

        return new AuthResult(newAccessToken, newRefreshTokenStr, user.getId(),
                              user.getName(), user.getEmail());
    }

    @Override
    public void logout(String refreshTokenStr) {
        refreshTokenRepo.findByToken(refreshTokenStr)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepo.save(token);
                });
    }
}
```

- [ ] **Step 7: Write tests for UserRegistrationService**

Create `auth-service/src/test/java/com/findash/auth/domain/service/UserRegistrationServiceTest.java`:

```java
package com.findash.auth.domain.service;

import com.findash.auth.domain.model.User;
import com.findash.auth.domain.port.out.PasswordEncoderPort;
import com.findash.auth.domain.port.out.UserRepositoryPort;
import com.findash.shared.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock private UserRepositoryPort userRepo;
    @Mock private PasswordEncoderPort passwordEncoder;

    private UserRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new UserRegistrationService(userRepo, passwordEncoder);
    }

    @Test
    void register_withNewEmail_createsUser() {
        when(userRepo.existsByEmail("new@email.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = service.register("Test User", "new@email.com", "password");

        assertNotNull(result);
        assertEquals("Test User", result.getName());
        assertEquals("new@email.com", result.getEmail());
        assertEquals("encoded", result.getPassword());
        assertTrue(result.isActive());
    }

    @Test
    void register_withExistingEmail_throwsDuplicateException() {
        when(userRepo.existsByEmail("existing@email.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> service.register("Test", "existing@email.com", "password"));
    }
}
```

- [ ] **Step 8: Implement UserRegistrationService**

Create `auth-service/src/main/java/com/findash/auth/domain/service/UserRegistrationService.java`:

```java
package com.findash.auth.domain.service;

import com.findash.auth.domain.model.User;
import com.findash.auth.domain.port.in.RegisterUserUseCase;
import com.findash.auth.domain.port.out.PasswordEncoderPort;
import com.findash.auth.domain.port.out.UserRepositoryPort;
import com.findash.shared.exception.DuplicateResourceException;

import java.time.Instant;

public class UserRegistrationService implements RegisterUserUseCase {

    private final UserRepositoryPort userRepo;
    private final PasswordEncoderPort passwordEncoder;

    public UserRegistrationService(UserRepositoryPort userRepo, PasswordEncoderPort passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(String name, String email, String password) {
        if (userRepo.existsByEmail(email)) {
            throw new DuplicateResourceException("Email ja cadastrado: " + email);
        }

        User user = new User(null, name, email, passwordEncoder.encode(password),
                              true, Instant.now(), Instant.now());
        return userRepo.save(user);
    }
}
```

- [ ] **Step 9: Run domain tests**

```bash
cd gestao-empresarial-backend && ./mvnw test -pl auth-service
```
Expected: All tests PASS.

- [ ] **Step 10: Commit**

```bash
git add gestao-empresarial-backend/auth-service/src/
git commit -m "feat(auth): add domain layer - models, ports, and services with TDD"
```

---

### Task 5: Auth Service — Persistence Layer

**Files:**
- Create: `auth-service/src/main/java/com/findash/auth/adapter/out/persistence/` (JPA entities, repos, adapters)
- Create: `auth-service/src/main/resources/db/migration/` (Flyway migrations)
- Create: `auth-service/src/main/resources/application.yml`

- [ ] **Step 1: Create Flyway migrations**

Create `auth-service/src/main/resources/db/migration/V1__create_users_table.sql`:

```sql
CREATE TABLE auth_schema.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE auth_schema.user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'EDITOR', 'VIEWER')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, company_id)
);

CREATE TABLE auth_schema.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON auth_schema.users(email);
CREATE INDEX idx_user_roles_user_id ON auth_schema.user_roles(user_id);
CREATE INDEX idx_user_roles_company_id ON auth_schema.user_roles(company_id);
CREATE INDEX idx_refresh_tokens_token ON auth_schema.refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON auth_schema.refresh_tokens(user_id);
```

- [ ] **Step 2: Create application.yml**

Create `auth-service/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:postgresql://localhost:5432/findash?currentSchema=auth_schema
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
  port: 8081

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

- [ ] **Step 3: Create JPA entities**

Create `auth-service/src/main/java/com/findash/auth/adapter/out/persistence/entity/UserEntity.java`:

```java
package com.findash.auth.adapter.out.persistence.entity;

import com.findash.shared.persistence.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "users", schema = "auth_schema")
public class UserEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean active = true;

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

Create `auth-service/src/main/java/com/findash/auth/adapter/out/persistence/entity/UserRoleEntity.java`:

```java
package com.findash.auth.adapter.out.persistence.entity;

import com.findash.auth.domain.model.Role;
import com.findash.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_roles", schema = "auth_schema",
       uniqueConstraints = @UniqueConstraint(columns = {"user_id", "company_id"}))
public class UserRoleEntity extends BaseEntity {

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

Create `auth-service/src/main/java/com/findash/auth/adapter/out/persistence/entity/RefreshTokenEntity.java`:

```java
package com.findash.auth.adapter.out.persistence.entity;

import com.findash.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", schema = "auth_schema")
public class RefreshTokenEntity extends BaseEntity {

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
}
```

- [ ] **Step 4: Create Spring Data repositories**

Create `auth-service/src/main/java/com/findash/auth/adapter/out/persistence/repository/JpaUserRepository.java`:

```java
package com.findash.auth.adapter.out.persistence.repository;

import com.findash.auth.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Create `auth-service/src/main/java/com/findash/auth/adapter/out/persistence/repository/JpaUserRoleRepository.java`:

```java
package com.findash.auth.adapter.out.persistence.repository;

import com.findash.auth.adapter.out.persistence.entity.UserRoleEntity;
import com.findash.auth.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JpaUserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {
    List<UserRoleEntity> findByUserId(UUID userId);
    List<UserRoleEntity> findByCompanyId(UUID companyId);
    void deleteByUserIdAndCompanyId(UUID userId, UUID companyId);
    boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId);
    long countByCompanyIdAndRole(UUID companyId, Role role);
}
```

Create `auth-service/src/main/java/com/findash/auth/adapter/out/persistence/repository/JpaRefreshTokenRepository.java`:

```java
package com.findash.auth.adapter.out.persistence.repository;

import com.findash.auth.adapter.out.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    void revokeAllByUserId(UUID userId);
}
```

- [ ] **Step 5: Create persistence adapters (implement domain ports)**

Create `auth-service/src/main/java/com/findash/auth/adapter/out/persistence/adapter/UserPersistenceAdapter.java`:

```java
package com.findash.auth.adapter.out.persistence.adapter;

import com.findash.auth.adapter.out.persistence.entity.UserEntity;
import com.findash.auth.adapter.out.persistence.repository.JpaUserRepository;
import com.findash.auth.domain.model.User;
import com.findash.auth.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final JpaUserRepository jpaRepo;

    public UserPersistenceAdapter(JpaUserRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepo.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepo.existsByEmail(email);
    }

    private UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setActive(user.isActive());
        return entity;
    }

    private User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getName(), entity.getEmail(),
                        entity.getPassword(), entity.isActive(),
                        entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
```

Create similar adapters for `UserRolePersistenceAdapter` and `RefreshTokenPersistenceAdapter` following the same pattern. Each implements its respective port, converting between domain models and JPA entities.

Create `auth-service/src/main/java/com/findash/auth/adapter/out/persistence/adapter/UserRolePersistenceAdapter.java`:

```java
package com.findash.auth.adapter.out.persistence.adapter;

import com.findash.auth.adapter.out.persistence.entity.UserRoleEntity;
import com.findash.auth.adapter.out.persistence.repository.JpaUserRoleRepository;
import com.findash.auth.domain.model.Role;
import com.findash.auth.domain.model.UserRole;
import com.findash.auth.domain.port.out.UserRoleRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class UserRolePersistenceAdapter implements UserRoleRepositoryPort {

    private final JpaUserRoleRepository jpaRepo;

    public UserRolePersistenceAdapter(JpaUserRoleRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public UserRole save(UserRole userRole) {
        UserRoleEntity entity = toEntity(userRole);
        UserRoleEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<UserRole> findByUserId(UUID userId) {
        return jpaRepo.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<UserRole> findByCompanyId(UUID companyId) {
        return jpaRepo.findByCompanyId(companyId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByUserIdAndCompanyId(UUID userId, UUID companyId) {
        jpaRepo.deleteByUserIdAndCompanyId(userId, companyId);
    }

    @Override
    public boolean existsByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return jpaRepo.existsByUserIdAndCompanyId(userId, companyId);
    }

    @Override
    public long countByCompanyIdAndRole(UUID companyId, Role role) {
        return jpaRepo.countByCompanyIdAndRole(companyId, role);
    }

    private UserRoleEntity toEntity(UserRole ur) {
        UserRoleEntity entity = new UserRoleEntity();
        entity.setId(ur.getId());
        entity.setUserId(ur.getUserId());
        entity.setCompanyId(ur.getCompanyId());
        entity.setRole(ur.getRole());
        return entity;
    }

    private UserRole toDomain(UserRoleEntity entity) {
        return new UserRole(entity.getId(), entity.getUserId(), entity.getCompanyId(),
                            entity.getRole(), entity.getCreatedAt());
    }
}
```

Create `auth-service/src/main/java/com/findash/auth/adapter/out/persistence/adapter/RefreshTokenPersistenceAdapter.java`:

```java
package com.findash.auth.adapter.out.persistence.adapter;

import com.findash.auth.adapter.out.persistence.entity.RefreshTokenEntity;
import com.findash.auth.adapter.out.persistence.repository.JpaRefreshTokenRepository;
import com.findash.auth.domain.model.RefreshToken;
import com.findash.auth.domain.port.out.RefreshTokenRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final JpaRefreshTokenRepository jpaRepo;

    public RefreshTokenPersistenceAdapter(JpaRefreshTokenRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity entity = toEntity(token);
        RefreshTokenEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepo.findByToken(token).map(this::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId) {
        jpaRepo.revokeAllByUserId(userId);
    }

    private RefreshTokenEntity toEntity(RefreshToken token) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setToken(token.getToken());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setRevoked(token.isRevoked());
        return entity;
    }

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(entity.getId(), entity.getUserId(), entity.getToken(),
                                entity.getExpiresAt(), entity.isRevoked(), entity.getCreatedAt());
    }
}
```

- [ ] **Step 6: Create PasswordEncoder adapter**

Create `auth-service/src/main/java/com/findash/auth/adapter/out/security/BcryptPasswordEncoderAdapter.java`:

```java
package com.findash.auth.adapter.out.security;

import com.findash.auth.domain.port.out.PasswordEncoderPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add gestao-empresarial-backend/auth-service/
git commit -m "feat(auth): add persistence layer - JPA entities, repos, Flyway migration, adapters"
```

---

### Task 6: Auth Service — Spring Config & Web Layer

**Files:**
- Create: `auth-service/src/main/java/com/findash/auth/config/`
- Create: `auth-service/src/main/java/com/findash/auth/adapter/in/web/`
- Create: `auth-service/src/main/java/com/findash/auth/AuthServiceApplication.java`

- [ ] **Step 1: Create Spring Security config**

Create `auth-service/src/main/java/com/findash/auth/config/SecurityConfig.java`:

```java
package com.findash.auth.config;

import com.findash.shared.security.JwtAuthenticationFilter;
import com.findash.shared.security.JwtTokenProvider;
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

- [ ] **Step 2: Create JwtTokenProvider bean config**

Create `auth-service/src/main/java/com/findash/auth/config/JwtConfig.java`:

```java
package com.findash.auth.config;

import com.findash.shared.security.JwtTokenProvider;
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

- [ ] **Step 3: Create DomainConfig (wires domain services as Spring beans)**

Create `auth-service/src/main/java/com/findash/auth/config/DomainConfig.java`:

```java
package com.findash.auth.config;

import com.findash.auth.domain.port.out.*;
import com.findash.auth.domain.service.AuthService;
import com.findash.auth.domain.service.UserRegistrationService;
import com.findash.shared.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public AuthService authService(UserRepositoryPort userRepo,
                                    UserRoleRepositoryPort userRoleRepo,
                                    RefreshTokenRepositoryPort refreshTokenRepo,
                                    PasswordEncoderPort passwordEncoder,
                                    JwtTokenProvider tokenProvider,
                                    @Value("${app.jwt.refresh-token-expiration-ms}") long refreshExpMs) {
        return new AuthService(userRepo, userRoleRepo, refreshTokenRepo,
                               passwordEncoder, tokenProvider, refreshExpMs);
    }

    @Bean
    public UserRegistrationService userRegistrationService(UserRepositoryPort userRepo,
                                                            PasswordEncoderPort passwordEncoder) {
        return new UserRegistrationService(userRepo, passwordEncoder);
    }
}
```

- [ ] **Step 4: Create request/response DTOs**

Create `auth-service/src/main/java/com/findash/auth/adapter/in/web/dto/request/RegisterRequest.java`:

```java
package com.findash.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Nome e obrigatorio")
    String name,

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email,

    @NotBlank(message = "Senha e obrigatoria")
    @Size(min = 6, message = "Senha deve ter no minimo 6 caracteres")
    String password
) {}
```

Create `auth-service/src/main/java/com/findash/auth/adapter/in/web/dto/request/LoginRequest.java`:

```java
package com.findash.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    String email,

    @NotBlank(message = "Senha e obrigatoria")
    String password
) {}
```

Create `auth-service/src/main/java/com/findash/auth/adapter/in/web/dto/request/RefreshRequest.java`:

```java
package com.findash.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "Refresh token e obrigatorio")
    String refreshToken
) {}
```

Create `auth-service/src/main/java/com/findash/auth/adapter/in/web/dto/response/AuthResponse.java`:

```java
package com.findash.auth.adapter.in.web.dto.response;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserResponse user
) {
    public record UserResponse(UUID id, String name, String email) {}
}
```

- [ ] **Step 5: Create AuthController**

Create `auth-service/src/main/java/com/findash/auth/adapter/in/web/AuthController.java`:

```java
package com.findash.auth.adapter.in.web;

import com.findash.auth.adapter.in.web.dto.request.*;
import com.findash.auth.adapter.in.web.dto.response.AuthResponse;
import com.findash.auth.adapter.in.web.dto.response.AuthResponse.UserResponse;
import com.findash.auth.domain.model.User;
import com.findash.auth.domain.port.in.AuthenticateUseCase;
import com.findash.auth.domain.port.in.AuthenticateUseCase.AuthResult;
import com.findash.auth.domain.port.in.RegisterUserUseCase;
import com.findash.shared.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUseCase;
    private final AuthenticateUseCase authUseCase;

    public AuthController(RegisterUserUseCase registerUseCase, AuthenticateUseCase authUseCase) {
        this.registerUseCase = registerUseCase;
        this.authUseCase = authUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUseCase.register(request.name(), request.email(), request.password());
        AuthResult result = authUseCase.authenticate(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authUseCase.authenticate(request.email(), request.password());
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResult result = authUseCase.refreshToken(request.refreshToken());
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authUseCase.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserContext user) {
        return ResponseEntity.ok(new UserResponse(user.userId(), null, user.email()));
    }

    private AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(
            result.accessToken(),
            result.refreshToken(),
            new UserResponse(result.userId(), result.name(), result.email())
        );
    }
}
```

- [ ] **Step 6: Create CorsConfig**

Create `auth-service/src/main/java/com/findash/auth/config/CorsConfig.java`:

```java
package com.findash.auth.config;

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

- [ ] **Step 7: Create Application main class**

Create `auth-service/src/main/java/com/findash/auth/AuthServiceApplication.java`:

```java
package com.findash.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.findash.auth", "com.findash.shared"})
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

- [ ] **Step 8: Verify Auth Service starts (requires PostgreSQL running)**

```bash
docker-compose up -d postgres
cd gestao-empresarial-backend && ./mvnw spring-boot:run -pl auth-service
```
Expected: App starts on port 8081, Flyway runs migration, Swagger at http://localhost:8081/swagger-ui.html

- [ ] **Step 9: Commit**

```bash
git add gestao-empresarial-backend/auth-service/
git commit -m "feat(auth): add web layer, Spring Security, controllers, and config"
```

---

## Chunk 3: API Gateway + Frontend

### Task 7: API Gateway

**Files:**
- Create: `api-gateway/src/main/java/com/findash/gateway/`
- Create: `api-gateway/src/main/resources/application.yml`

- [ ] **Step 1: Create Gateway application**

Create `api-gateway/src/main/java/com/findash/gateway/GatewayApplication.java`:

```java
package com.findash.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

- [ ] **Step 2: Create Gateway configuration**

Create `api-gateway/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      mvc:
        routes:
          - id: auth-service
            uri: http://localhost:8081
            predicates:
              - Path=/api/auth/**
          - id: company-service
            uri: http://localhost:8082
            predicates:
              - Path=/api/companies/**
          - id: financial-service
            uri: http://localhost:8083
            predicates:
              - Path=/api/financial/**

server:
  port: 8080
```

- [ ] **Step 3: Add CORS config for Gateway**

Create `api-gateway/src/main/java/com/findash/gateway/config/GatewayCorsConfig.java`:

```java
package com.findash.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class GatewayCorsConfig {

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

- [ ] **Step 4: Verify Gateway compiles and routes to Auth**

```bash
cd gestao-empresarial-backend && ./mvnw clean compile -pl api-gateway
```
Expected: BUILD SUCCESS

With both auth-service and gateway running, test:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","password":"123456"}'
```
Expected: 201 Created with JWT response.

- [ ] **Step 5: Commit**

```bash
git add gestao-empresarial-backend/api-gateway/
git commit -m "feat(gateway): add Spring Cloud Gateway MVC with route config"
```

---

### Task 8: Frontend — Install PrimeNG + Setup

**Files:**
- Modify: `gestao-empresaial-frontend/package.json`
- Modify: `gestao-empresaial-frontend/src/styles.css`
- Modify: `gestao-empresaial-frontend/src/app/app.config.ts`

- [ ] **Step 1: Install PrimeNG and dependencies**

```bash
cd gestao-empresaial-frontend
npm install primeng @primeng/themes primeicons
npm install @angular/platform-browser-dynamic @angular/animations
```

- [ ] **Step 2: Update styles.css with fonts and PrimeNG**

Add to `gestao-empresaial-frontend/src/styles.css`:

```css
@import "tailwindcss";
@import "primeicons/primeicons.css";

@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;700&display=swap');

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #111827;
  background-color: #ffffff;
}

.font-mono {
  font-family: 'JetBrains Mono', ui-monospace, 'Courier New', monospace;
}
```

- [ ] **Step 3: Update app.config.ts with HttpClient and animations**

Replace `gestao-empresaial-frontend/src/app/app.config.ts`:

```typescript
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([])),
    provideAnimationsAsync(),
  ],
};
```

- [ ] **Step 4: Verify frontend builds**

```bash
cd gestao-empresaial-frontend && npm run build
```
Expected: Build succeeds.

- [ ] **Step 5: Commit**

```bash
git add gestao-empresaial-frontend/
git commit -m "feat(frontend): install PrimeNG, add fonts and base config"
```

---

### Task 9: Frontend — Layout Shell (Sidebar + Header)

**Files:**
- Create: `src/app/core/layout/components/sidebar/`
- Create: `src/app/core/layout/components/header/`
- Create: `src/app/core/layout/components/layout/`
- Modify: `src/app/app.routes.ts`
- Modify: `src/app/app.ts`
- Modify: `src/app/app.html`

- [ ] **Step 1: Create Sidebar component**

Create `gestao-empresaial-frontend/src/app/core/layout/components/sidebar/sidebar.ts`:

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block w-64 min-h-screen bg-gray-50 border-r border-gray-200' },
  template: `
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
  `,
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

- [ ] **Step 2: Create Header component**

Create `gestao-empresaial-frontend/src/app/core/layout/components/header/header.ts`:

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-header',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block h-16 bg-white border-b border-gray-200' },
  template: `
    <div class="flex items-center justify-between h-full px-6">
      <div></div>
      <div class="flex items-center gap-4">
        <button class="flex items-center gap-2 px-3 py-1.5 text-sm border border-gray-200 rounded-md hover:bg-gray-50">
          <i class="pi pi-building text-gray-500"></i>
          <span class="text-gray-700">Selecionar empresa</span>
          <i class="pi pi-chevron-down text-xs text-gray-400"></i>
        </button>
        <button class="flex items-center gap-2 px-3 py-1.5 text-sm rounded-md hover:bg-gray-50">
          <i class="pi pi-user text-gray-500"></i>
          <span class="text-gray-700">Perfil</span>
        </button>
      </div>
    </div>
  `,
})
export class HeaderComponent {}
```

- [ ] **Step 3: Create Layout component**

Create `gestao-empresaial-frontend/src/app/core/layout/components/layout/layout.ts`:

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar';
import { HeaderComponent } from '../header/header';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, SidebarComponent, HeaderComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex min-h-screen">
      <app-sidebar />
      <div class="flex-1 flex flex-col">
        <app-header />
        <main class="flex-1 p-8">
          <div class="max-w-7xl mx-auto">
            <router-outlet />
          </div>
        </main>
      </div>
    </div>
  `,
})
export class LayoutComponent {}
```

- [ ] **Step 4: Create placeholder Dashboard component**

Create `gestao-empresaial-frontend/src/app/features/dashboard/components/dashboard/dashboard.ts`:

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-3xl font-bold text-gray-900">Dashboard</h1>
    <p class="mt-2 text-sm text-gray-500">Bem-vindo ao FinDash. Seus dados aparecerão aqui.</p>
  `,
})
export class DashboardComponent {}
```

- [ ] **Step 5: Update app routes**

Replace `gestao-empresaial-frontend/src/app/app.routes.ts`:

```typescript
import { Routes } from '@angular/router';
import { LayoutComponent } from './core/layout/components/layout/layout';

export const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/components/dashboard/dashboard').then(
            (m) => m.DashboardComponent
          ),
      },
    ],
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/components/login/login').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/components/register/register').then((m) => m.RegisterComponent),
  },
];
```

- [ ] **Step 6: Update app root component**

Replace `gestao-empresaial-frontend/src/app/app.html`:

```html
<router-outlet />
```

Replace `gestao-empresaial-frontend/src/app/app.ts`:

```typescript
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
```

- [ ] **Step 7: Verify frontend builds and layout renders**

```bash
cd gestao-empresaial-frontend && npm run build
```
Expected: Build succeeds. Running `npm start` shows sidebar + header + "Dashboard" content.

- [ ] **Step 8: Commit**

```bash
git add gestao-empresaial-frontend/
git commit -m "feat(frontend): add layout shell with sidebar, header, and routing"
```

---

### Task 10: Frontend — Auth Service + Login/Register Pages

**Files:**
- Create: `src/app/core/auth/services/auth.service.ts`
- Create: `src/app/core/auth/interceptors/auth.interceptor.ts`
- Create: `src/app/core/auth/guards/auth.guard.ts`
- Create: `src/app/features/auth/components/login/`
- Create: `src/app/features/auth/components/register/`
- Modify: `src/app/app.config.ts`

- [ ] **Step 1: Create AuthService**

Create `gestao-empresaial-frontend/src/app/core/auth/services/auth.service.ts`:

```typescript
import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: { id: string; name: string; email: string };
}

interface UserInfo {
  id: string;
  name: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _accessToken = signal<string | null>(null);
  private readonly _user = signal<UserInfo | null>(null);

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._accessToken() !== null);

  private readonly API_URL = '/api/auth';

  get accessToken(): string | null {
    return this._accessToken();
  }

  login(email: string, password: string) {
    return this.http
      .post<AuthResponse>(`${this.API_URL}/login`, { email, password })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  register(name: string, email: string, password: string) {
    return this.http
      .post<AuthResponse>(`${this.API_URL}/register`, { name, email, password })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  refreshToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return;

    return this.http
      .post<AuthResponse>(`${this.API_URL}/refresh`, { refreshToken })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  logout() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      this.http.post(`${this.API_URL}/logout`, { refreshToken }).subscribe();
    }
    this._accessToken.set(null);
    this._user.set(null);
    localStorage.removeItem('refreshToken');
    this.router.navigate(['/login']);
  }

  private handleAuthResponse(res: AuthResponse) {
    this._accessToken.set(res.accessToken);
    this._user.set(res.user);
    localStorage.setItem('refreshToken', res.refreshToken);
  }
}

import { inject } from '@angular/core';
```

- [ ] **Step 2: Create auth interceptor**

Create `gestao-empresaial-frontend/src/app/core/auth/interceptors/auth.interceptor.ts`:

```typescript
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.accessToken;

  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/')) {
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};
```

- [ ] **Step 3: Create auth guard**

Create `gestao-empresaial-frontend/src/app/core/auth/guards/auth.guard.ts`:

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};
```

- [ ] **Step 4: Wire interceptor in app.config.ts**

Update `gestao-empresaial-frontend/src/app/app.config.ts`:

```typescript
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { authInterceptor } from './core/auth/interceptors/auth.interceptor';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
  ],
};
```

- [ ] **Step 5: Create Login component**

Create `gestao-empresaial-frontend/src/app/features/auth/components/login/login.ts`:

```typescript
import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50">
      <div class="w-full max-w-md p-8 bg-white rounded-lg border border-gray-200">
        <h1 class="text-2xl font-bold text-gray-900 mb-2">Entrar no FinDash</h1>
        <p class="text-sm text-gray-500 mb-6">Acesse sua conta para continuar</p>

        @if (error()) {
          <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
            {{ error() }}
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              formControlName="email"
              type="email"
              class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="seu@email.com"
            />
          </div>

          <div class="mb-6">
            <label class="block text-sm font-medium text-gray-700 mb-1">Senha</label>
            <input
              formControlName="password"
              type="password"
              class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Sua senha"
            />
          </div>

          <button
            type="submit"
            [disabled]="form.invalid || loading()"
            class="w-full py-2 px-4 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading() ? 'Entrando...' : 'Entrar' }}
          </button>
        </form>

        <p class="mt-4 text-center text-sm text-gray-500">
          Nao tem conta?
          <a routerLink="/register" class="text-blue-600 hover:text-blue-700 font-medium">
            Criar conta
          </a>
        </p>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected submit() {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { email, password } = this.form.getRawValue();
    this.authService.login(email, password).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao fazer login');
      },
    });
  }
}
```

- [ ] **Step 6: Create Register component**

Create `gestao-empresaial-frontend/src/app/features/auth/components/register/register.ts`:

```typescript
import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/services/auth.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50">
      <div class="w-full max-w-md p-8 bg-white rounded-lg border border-gray-200">
        <h1 class="text-2xl font-bold text-gray-900 mb-2">Criar conta</h1>
        <p class="text-sm text-gray-500 mb-6">Comece a usar o FinDash gratuitamente</p>

        @if (error()) {
          <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
            {{ error() }}
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">Nome</label>
            <input
              formControlName="name"
              type="text"
              class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Seu nome completo"
            />
          </div>

          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              formControlName="email"
              type="email"
              class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="seu@email.com"
            />
          </div>

          <div class="mb-6">
            <label class="block text-sm font-medium text-gray-700 mb-1">Senha</label>
            <input
              formControlName="password"
              type="password"
              class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Minimo 6 caracteres"
            />
          </div>

          <button
            type="submit"
            [disabled]="form.invalid || loading()"
            class="w-full py-2 px-4 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading() ? 'Criando conta...' : 'Criar conta' }}
          </button>
        </form>

        <p class="mt-4 text-center text-sm text-gray-500">
          Ja tem conta?
          <a routerLink="/login" class="text-blue-600 hover:text-blue-700 font-medium">
            Entrar
          </a>
        </p>
      </div>
    </div>
  `,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  protected submit() {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { name, email, password } = this.form.getRawValue();
    this.authService.register(name, email, password).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao criar conta');
      },
    });
  }
}
```

- [ ] **Step 7: Add authGuard to protected routes**

Update route config in `app.routes.ts` — add `canActivate: [authGuard]` to the layout route:

```typescript
import { Routes } from '@angular/router';
import { LayoutComponent } from './core/layout/components/layout/layout';
import { authGuard } from './core/auth/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/components/dashboard/dashboard').then(
            (m) => m.DashboardComponent
          ),
      },
    ],
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/components/login/login').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/components/register/register').then((m) => m.RegisterComponent),
  },
];
```

- [ ] **Step 8: Add proxy config for dev server**

Create `gestao-empresaial-frontend/proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

Update `gestao-empresaial-frontend/angular.json` — add proxy to serve config:
In the `serve` architect options, add `"proxyConfig": "proxy.conf.json"`.

- [ ] **Step 9: Verify frontend builds**

```bash
cd gestao-empresaial-frontend && npm run build
```
Expected: Build succeeds.

- [ ] **Step 10: Commit**

```bash
git add gestao-empresaial-frontend/
git commit -m "feat(frontend): add auth service, login/register pages, interceptor, and guard"
```

---

## Chunk 4: Integration Verification

### Task 11: End-to-End Smoke Test

- [ ] **Step 1: Start all services**

```bash
# Terminal 1: PostgreSQL
docker-compose up -d postgres

# Terminal 2: Auth Service
cd gestao-empresarial-backend && ./mvnw spring-boot:run -pl auth-service

# Terminal 3: API Gateway
cd gestao-empresarial-backend && ./mvnw spring-boot:run -pl api-gateway

# Terminal 4: Frontend
cd gestao-empresaial-frontend && npm start
```

- [ ] **Step 2: Verify register flow**

1. Open http://localhost:4200 — should redirect to /login
2. Click "Criar conta"
3. Fill form and submit
4. Should redirect to /dashboard with sidebar visible

- [ ] **Step 3: Verify login flow**

1. Open http://localhost:4200/login
2. Login with created credentials
3. Should redirect to /dashboard

- [ ] **Step 4: Verify Swagger UI**

Open http://localhost:8081/swagger-ui.html — should show Auth Service API docs.

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat: complete Phase 1 - infrastructure, auth service, and frontend shell"
```

---

## Summary

Phase 1 delivers:
- Docker Compose with PostgreSQL (3 schemas)
- Multi-module Maven backend (parent + shared + auth-service + api-gateway)
- Auth Service with hexagonal architecture (domain, persistence, web layers)
- JWT authentication (register, login, refresh, logout)
- Spring Cloud Gateway MVC routing
- Angular frontend with layout shell (sidebar + header)
- Login and register pages
- Auth interceptor, guard, and service

**Next:** Phase 2 plan (Company Service + Multi-empresa) — to be created as a separate plan.
