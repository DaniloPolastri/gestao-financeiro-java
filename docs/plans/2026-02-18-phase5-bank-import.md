# Phase 5: Importação OFX/CSV — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implementar importação de extratos bancários OFX/CSV com parsing síncrono, tela de revisão com vínculo a fornecedor/categoria, detecção de duplicados e confirmação que gera contas a pagar/receber.

**Architecture:** Upload síncrono — arquivo é parseado na mesma request e os itens são salvos em `bank_import_items`. O usuário revisa na tela de revisão, edita vínculos (inline ou em lote) e confirma, gerando `Account` por item e salvando regras de matching em `supplier_match_rules`.

**Tech Stack:** Java 17, Spring Boot 4, ofx4j (parsing OFX), Apache Commons CSV (parsing CSV), Angular 21, PrimeNG FileUpload + Table, Signals.

**Design Spec:** `docs/specs/2026-02-18-phase5-bank-import-design.md`

---

## Checklist de Progresso

- [ ] Task 1: Branch + dependências Maven
- [ ] Task 2: Flyway migration V6
- [ ] Task 3: Entidades JPA + enums
- [ ] Task 4: Repositories
- [ ] Task 5: DTOs
- [ ] Task 6: Parser OFX
- [ ] Task 7: Parser CSV (auto-detect + template fallback)
- [ ] Task 8: BankImportService — upload e parsing
- [ ] Task 9: BankImportService — edição de itens (individual + batch)
- [ ] Task 10: BankImportService — confirmar importação
- [ ] Task 11: BankImportService — cancelar, listar, buscar por ID
- [ ] Task 12: BankImportController
- [ ] Task 13: Testes do BankImportService
- [ ] Task 14: Frontend — models e BankImportService
- [ ] Task 15: Frontend — rotas e navegação
- [ ] Task 16: Frontend — BankImportUploadComponent
- [ ] Task 17: Frontend — BankImportReviewComponent
- [ ] Task 18: Frontend — BankImportListComponent
- [ ] Task 19: Template CSV para download
- [ ] Task 20: Commit final e push

---

## Task 1: Branch + dependências Maven

**Files:**
- Modify: `gestao-empresarial-backend/pom.xml`

- [ ] Criar branch a partir da master:
  ```bash
  git checkout master && git pull origin master
  git checkout -b feature/phase5-bank-import
  ```

- [ ] Adicionar dependências no `pom.xml` (dentro de `<dependencies>`):
  ```xml
  <!-- OFX parsing -->
  <dependency>
      <groupId>net.sf.ofx4j</groupId>
      <artifactId>ofx4j</artifactId>
      <version>1.8</version>
  </dependency>

  <!-- CSV parsing -->
  <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-csv</artifactId>
      <version>1.12.0</version>
  </dependency>
  ```

- [ ] Verificar que o build compila:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```
  Esperado: `BUILD SUCCESS`

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/pom.xml
  git commit -m "build: add ofx4j and commons-csv dependencies for bank import"
  ```

---

## Task 2: Flyway migration V6

**Files:**
- Create: `gestao-empresarial-backend/src/main/resources/db/migration/V6__create_bank_import_tables.sql`

- [ ] Criar o arquivo de migration:
  ```sql
  CREATE TABLE financial_schema.bank_imports (
      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      company_id  UUID NOT NULL,
      file_name   VARCHAR(255) NOT NULL,
      file_type   VARCHAR(10) NOT NULL CHECK (file_type IN ('OFX', 'CSV')),
      status      VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW'
                      CHECK (status IN ('PENDING_REVIEW', 'COMPLETED', 'CANCELLED')),
      total_records INT NOT NULL DEFAULT 0,
      imported_by UUID,
      created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE financial_schema.bank_import_items (
      id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      import_id          UUID NOT NULL REFERENCES financial_schema.bank_imports(id) ON DELETE CASCADE,
      date               DATE NOT NULL,
      description        VARCHAR(500) NOT NULL,
      amount             DECIMAL(15, 2) NOT NULL,
      type               VARCHAR(10) NOT NULL CHECK (type IN ('CREDIT', 'DEBIT')),
      account_type       VARCHAR(10) NOT NULL CHECK (account_type IN ('PAYABLE', 'RECEIVABLE')),
      supplier_id        UUID,
      category_id        UUID,
      possible_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
      original_data      JSONB,
      created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE financial_schema.supplier_match_rules (
      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      company_id  UUID NOT NULL,
      pattern     VARCHAR(255) NOT NULL,
      supplier_id UUID NOT NULL,
      category_id UUID,
      created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      UNIQUE (company_id, pattern)
  );

  CREATE INDEX idx_bank_imports_company_id ON financial_schema.bank_imports(company_id);
  CREATE INDEX idx_bank_import_items_import_id ON financial_schema.bank_import_items(import_id);
  CREATE INDEX idx_supplier_match_rules_company_id ON financial_schema.supplier_match_rules(company_id);
  ```

- [ ] Subir o backend para validar a migration (requer PostgreSQL rodando):
  ```bash
  cd gestao-empresarial-backend && ./mvnw spring-boot:run
  ```
  Esperado: `Flyway: Successfully applied 1 migration` nos logs. Parar com Ctrl+C.

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/resources/db/migration/V6__create_bank_import_tables.sql
  git commit -m "feat(db): add bank_imports, bank_import_items, supplier_match_rules tables"
  ```

---

## Task 3: Entidades JPA + enums

**Files:**
- Create: `...entity/BankImportStatus.java`
- Create: `...entity/BankImportFileType.java`
- Create: `...entity/BankImportItemType.java`
- Create: `...entity/BankImport.java`
- Create: `...entity/BankImportItem.java`
- Create: `...entity/SupplierMatchRule.java`

Prefixo de path: `gestao-empresarial-backend/src/main/java/com/findash/`

- [ ] Criar `entity/BankImportStatus.java`:
  ```java
  package com.findash.entity;
  public enum BankImportStatus { PENDING_REVIEW, COMPLETED, CANCELLED }
  ```

- [ ] Criar `entity/BankImportFileType.java`:
  ```java
  package com.findash.entity;
  public enum BankImportFileType { OFX, CSV }
  ```

- [ ] Criar `entity/BankImportItemType.java`:
  ```java
  package com.findash.entity;
  public enum BankImportItemType { CREDIT, DEBIT }
  ```

- [ ] Criar `entity/BankImport.java`:
  ```java
  package com.findash.entity;

  import jakarta.persistence.*;
  import java.time.LocalDateTime;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.UUID;

  @Entity
  @Table(name = "bank_imports", schema = "financial_schema")
  public class BankImport {

      @Id
      @GeneratedValue(strategy = GenerationType.UUID)
      private UUID id;

      @Column(name = "company_id", nullable = false)
      private UUID companyId;

      @Column(name = "file_name", nullable = false)
      private String fileName;

      @Enumerated(EnumType.STRING)
      @Column(name = "file_type", nullable = false)
      private BankImportFileType fileType;

      @Enumerated(EnumType.STRING)
      @Column(nullable = false)
      private BankImportStatus status = BankImportStatus.PENDING_REVIEW;

      @Column(name = "total_records")
      private int totalRecords;

      @Column(name = "imported_by")
      private UUID importedBy;

      @Column(name = "created_at")
      private LocalDateTime createdAt = LocalDateTime.now();

      @OneToMany(mappedBy = "importId", cascade = CascadeType.ALL, orphanRemoval = true)
      private List<BankImportItem> items = new ArrayList<>();

      protected BankImport() {}

      public BankImport(UUID companyId, String fileName, BankImportFileType fileType, UUID importedBy) {
          this.companyId = companyId;
          this.fileName = fileName;
          this.fileType = fileType;
          this.importedBy = importedBy;
      }

      public UUID getId() { return id; }
      public UUID getCompanyId() { return companyId; }
      public String getFileName() { return fileName; }
      public BankImportFileType getFileType() { return fileType; }
      public BankImportStatus getStatus() { return status; }
      public int getTotalRecords() { return totalRecords; }
      public UUID getImportedBy() { return importedBy; }
      public LocalDateTime getCreatedAt() { return createdAt; }
      public List<BankImportItem> getItems() { return items; }

      public void setStatus(BankImportStatus status) { this.status = status; }
      public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
      public void setId(UUID id) { this.id = id; }
  }
  ```

- [ ] Criar `entity/BankImportItem.java`:
  ```java
  package com.findash.entity;

  import jakarta.persistence.*;
  import org.hibernate.annotations.JdbcTypeCode;
  import org.hibernate.type.SqlTypes;

  import java.math.BigDecimal;
  import java.time.LocalDate;
  import java.time.LocalDateTime;
  import java.util.Map;
  import java.util.UUID;

  @Entity
  @Table(name = "bank_import_items", schema = "financial_schema")
  public class BankImportItem {

      @Id
      @GeneratedValue(strategy = GenerationType.UUID)
      private UUID id;

      @Column(name = "import_id", nullable = false)
      private UUID importId;

      @Column(nullable = false)
      private LocalDate date;

      @Column(nullable = false, length = 500)
      private String description;

      @Column(nullable = false, precision = 15, scale = 2)
      private BigDecimal amount;

      @Enumerated(EnumType.STRING)
      @Column(nullable = false)
      private BankImportItemType type;

      @Enumerated(EnumType.STRING)
      @Column(name = "account_type", nullable = false)
      private AccountType accountType;

      @Column(name = "supplier_id")
      private UUID supplierId;

      @Column(name = "category_id")
      private UUID categoryId;

      @Column(name = "possible_duplicate")
      private boolean possibleDuplicate = false;

      @JdbcTypeCode(SqlTypes.JSON)
      @Column(name = "original_data", columnDefinition = "jsonb")
      private Map<String, Object> originalData;

      @Column(name = "created_at")
      private LocalDateTime createdAt = LocalDateTime.now();

      protected BankImportItem() {}

      public BankImportItem(UUID importId, LocalDate date, String description,
                            BigDecimal amount, BankImportItemType type, AccountType accountType) {
          this.importId = importId;
          this.date = date;
          this.description = description;
          this.amount = amount;
          this.type = type;
          this.accountType = accountType;
      }

      public UUID getId() { return id; }
      public UUID getImportId() { return importId; }
      public LocalDate getDate() { return date; }
      public String getDescription() { return description; }
      public BigDecimal getAmount() { return amount; }
      public BankImportItemType getType() { return type; }
      public AccountType getAccountType() { return accountType; }
      public UUID getSupplierId() { return supplierId; }
      public UUID getCategoryId() { return categoryId; }
      public boolean isPossibleDuplicate() { return possibleDuplicate; }
      public Map<String, Object> getOriginalData() { return originalData; }
      public LocalDateTime getCreatedAt() { return createdAt; }

      public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
      public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
      public void setAccountType(AccountType accountType) { this.accountType = accountType; }
      public void setPossibleDuplicate(boolean possibleDuplicate) { this.possibleDuplicate = possibleDuplicate; }
      public void setOriginalData(Map<String, Object> originalData) { this.originalData = originalData; }
      public void setId(UUID id) { this.id = id; }
  }
  ```

- [ ] Criar `entity/SupplierMatchRule.java`:
  ```java
  package com.findash.entity;

  import jakarta.persistence.*;
  import java.time.LocalDateTime;
  import java.util.UUID;

  @Entity
  @Table(name = "supplier_match_rules", schema = "financial_schema")
  public class SupplierMatchRule {

      @Id
      @GeneratedValue(strategy = GenerationType.UUID)
      private UUID id;

      @Column(name = "company_id", nullable = false)
      private UUID companyId;

      @Column(nullable = false)
      private String pattern;

      @Column(name = "supplier_id", nullable = false)
      private UUID supplierId;

      @Column(name = "category_id")
      private UUID categoryId;

      @Column(name = "created_at")
      private LocalDateTime createdAt = LocalDateTime.now();

      protected SupplierMatchRule() {}

      public SupplierMatchRule(UUID companyId, String pattern, UUID supplierId, UUID categoryId) {
          this.companyId = companyId;
          this.pattern = pattern;
          this.supplierId = supplierId;
          this.categoryId = categoryId;
      }

      public UUID getId() { return id; }
      public UUID getCompanyId() { return companyId; }
      public String getPattern() { return pattern; }
      public UUID getSupplierId() { return supplierId; }
      public UUID getCategoryId() { return categoryId; }
      public LocalDateTime getCreatedAt() { return createdAt; }

      public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
      public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
  }
  ```

- [ ] Compilar para verificar erros:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```
  Esperado: `BUILD SUCCESS`

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/entity/
  git commit -m "feat(entity): add BankImport, BankImportItem, SupplierMatchRule entities"
  ```

---

## Task 4: Repositories

**Files:**
- Create: `...repository/BankImportRepository.java`
- Create: `...repository/BankImportItemRepository.java`
- Create: `...repository/SupplierMatchRuleRepository.java`

- [ ] Criar `repository/BankImportRepository.java`:
  ```java
  package com.findash.repository;

  import com.findash.entity.BankImport;
  import org.springframework.data.jpa.repository.JpaRepository;
  import java.util.List;
  import java.util.Optional;
  import java.util.UUID;

  public interface BankImportRepository extends JpaRepository<BankImport, UUID> {
      List<BankImport> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
      Optional<BankImport> findByIdAndCompanyId(UUID id, UUID companyId);
  }
  ```

- [ ] Criar `repository/BankImportItemRepository.java`:
  ```java
  package com.findash.repository;

  import com.findash.entity.BankImportItem;
  import org.springframework.data.jpa.repository.JpaRepository;
  import java.util.List;
  import java.util.Optional;
  import java.util.UUID;

  public interface BankImportItemRepository extends JpaRepository<BankImportItem, UUID> {
      List<BankImportItem> findByImportId(UUID importId);
      Optional<BankImportItem> findByIdAndImportId(UUID id, UUID importId);
      boolean existsByImportIdAndSupplierIdIsNullOrImportIdAndCategoryIdIsNull(UUID importId1, UUID importId2);
  }
  ```

- [ ] Criar `repository/SupplierMatchRuleRepository.java`:
  ```java
  package com.findash.repository;

  import com.findash.entity.SupplierMatchRule;
  import org.springframework.data.jpa.repository.JpaRepository;
  import java.util.List;
  import java.util.Optional;
  import java.util.UUID;

  public interface SupplierMatchRuleRepository extends JpaRepository<SupplierMatchRule, UUID> {
      List<SupplierMatchRule> findByCompanyId(UUID companyId);
      Optional<SupplierMatchRule> findByCompanyIdAndPattern(UUID companyId, String pattern);
  }
  ```

- [ ] Compilar:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/repository/BankImport*
  git add gestao-empresarial-backend/src/main/java/com/findash/repository/SupplierMatchRuleRepository.java
  git commit -m "feat(repository): add BankImportRepository, BankImportItemRepository, SupplierMatchRuleRepository"
  ```

---

## Task 5: DTOs

**Files:**
- Create: `...dto/bankimport/` (pasta com todos os DTOs abaixo)

- [ ] Criar `dto/bankimport/BankImportItemResponseDTO.java`:
  ```java
  package com.findash.dto.bankimport;

  import java.math.BigDecimal;
  import java.time.LocalDate;
  import java.util.UUID;

  public record BankImportItemResponseDTO(
      UUID id,
      LocalDate date,
      String description,
      BigDecimal amount,
      String type,
      String accountType,
      UUID supplierId,
      String supplierName,
      UUID categoryId,
      String categoryName,
      boolean possibleDuplicate
  ) {}
  ```

- [ ] Criar `dto/bankimport/BankImportResponseDTO.java`:
  ```java
  package com.findash.dto.bankimport;

  import java.time.LocalDateTime;
  import java.util.List;
  import java.util.UUID;

  public record BankImportResponseDTO(
      UUID id,
      String fileName,
      String fileType,
      String status,
      int totalRecords,
      LocalDateTime createdAt,
      List<BankImportItemResponseDTO> items
  ) {}
  ```

- [ ] Criar `dto/bankimport/BankImportSummaryDTO.java`:
  ```java
  package com.findash.dto.bankimport;

  import java.time.LocalDateTime;
  import java.util.UUID;

  public record BankImportSummaryDTO(
      UUID id,
      String fileName,
      String fileType,
      String status,
      int totalRecords,
      LocalDateTime createdAt
  ) {}
  ```

- [ ] Criar `dto/bankimport/UpdateImportItemRequestDTO.java`:
  ```java
  package com.findash.dto.bankimport;

  import java.util.UUID;

  public record UpdateImportItemRequestDTO(
      UUID supplierId,
      UUID categoryId,
      String accountType
  ) {}
  ```

- [ ] Criar `dto/bankimport/BatchUpdateImportItemsRequestDTO.java`:
  ```java
  package com.findash.dto.bankimport;

  import java.util.List;
  import java.util.UUID;

  public record BatchUpdateImportItemsRequestDTO(
      List<UUID> itemIds,
      UUID supplierId,
      UUID categoryId,
      String accountType
  ) {}
  ```

- [ ] Compilar:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/dto/bankimport/
  git commit -m "feat(dto): add bank import DTOs"
  ```

---

## Task 6: Parser OFX

**Files:**
- Create: `...service/parser/ParsedTransaction.java`
- Create: `...service/parser/BankStatementParser.java`
- Create: `...service/parser/OfxParser.java`

- [ ] Criar `service/parser/ParsedTransaction.java` (record interno compartilhado pelos parsers):
  ```java
  package com.findash.service.parser;

  import java.math.BigDecimal;
  import java.time.LocalDate;
  import java.util.Map;

  public record ParsedTransaction(
      LocalDate date,
      String description,
      BigDecimal amount,
      String type,       // "CREDIT" ou "DEBIT"
      Map<String, Object> rawData
  ) {}
  ```

- [ ] Criar `service/parser/BankStatementParser.java` (interface):
  ```java
  package com.findash.service.parser;

  import java.io.InputStream;
  import java.util.List;

  public interface BankStatementParser {
      List<ParsedTransaction> parse(InputStream input, String filename) throws Exception;
  }
  ```

- [ ] Criar `service/parser/OfxParser.java`:
  ```java
  package com.findash.service.parser;

  import net.sf.ofx4j.data.TransactionType;
  import net.sf.ofx4j.domain.data.banking.BankStatementResponse;
  import net.sf.ofx4j.domain.data.banking.BankTransactionList;
  import net.sf.ofx4j.domain.data.common.Transaction;
  import net.sf.ofx4j.io.AggregateUnmarshaller;
  import net.sf.ofx4j.domain.data.ResponseEnvelope;
  import net.sf.ofx4j.domain.data.banking.BankStatementResponseTransaction;
  import net.sf.ofx4j.domain.data.MessageSetType;
  import org.springframework.stereotype.Component;

  import java.io.InputStream;
  import java.math.BigDecimal;
  import java.time.ZoneId;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Map;

  @Component
  public class OfxParser implements BankStatementParser {

      @Override
      public List<ParsedTransaction> parse(InputStream input, String filename) throws Exception {
          AggregateUnmarshaller<ResponseEnvelope> unmarshaller =
              new AggregateUnmarshaller<>(ResponseEnvelope.class);
          ResponseEnvelope envelope = unmarshaller.unmarshal(input);

          var bankMessages = envelope.getMessageSet(MessageSetType.banking);
          if (bankMessages == null) {
              throw new IllegalArgumentException("Arquivo OFX nao contem transacoes bancarias");
          }

          List<ParsedTransaction> result = new ArrayList<>();
          for (var responseTransaction : ((net.sf.ofx4j.domain.data.banking.BankStatementResponseMessageSet) bankMessages).getStatementResponses()) {
              BankStatementResponse statement = responseTransaction.getMessage();
              BankTransactionList txList = statement.getTransactionList();
              if (txList == null) continue;

              for (Transaction tx : txList.getTransactions()) {
                  BigDecimal amount = BigDecimal.valueOf(Math.abs(tx.getAmount()));
                  String type = tx.getAmount() >= 0 ? "CREDIT" : "DEBIT";
                  LocalDate date = tx.getDatePosted()
                      .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                  String description = tx.getMemo() != null ? tx.getMemo() : tx.getName();

                  result.add(new ParsedTransaction(
                      date, description, amount, type,
                      Map.of("fitid", tx.getId() != null ? tx.getId() : "",
                             "memo", description)
                  ));
              }
          }
          return result;
      }
  }
  ```

- [ ] Compilar:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/service/parser/
  git commit -m "feat(parser): add OFX parser using ofx4j"
  ```

---

## Task 7: Parser CSV (auto-detect + template fallback)

**Files:**
- Create: `...service/parser/CsvParser.java`

- [ ] Criar `service/parser/CsvParser.java`:
  ```java
  package com.findash.service.parser;

  import com.findash.exception.BusinessRuleException;
  import org.apache.commons.csv.CSVFormat;
  import org.apache.commons.csv.CSVParser;
  import org.apache.commons.csv.CSVRecord;
  import org.springframework.stereotype.Component;

  import java.io.BufferedReader;
  import java.io.InputStream;
  import java.io.InputStreamReader;
  import java.math.BigDecimal;
  import java.nio.charset.Charset;
  import java.nio.charset.StandardCharsets;
  import java.time.LocalDate;
  import java.time.format.DateTimeFormatter;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Map;

  @Component
  public class CsvParser implements BankStatementParser {

      // Colunas esperadas pelo template padrao
      private static final String[] TEMPLATE_HEADERS = {"data", "descricao", "valor", "tipo"};
      private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

      @Override
      public List<ParsedTransaction> parse(InputStream input, String filename) throws Exception {
          byte[] bytes = input.readAllBytes();

          // Tenta UTF-8 primeiro, depois ISO-8859-1
          for (Charset charset : List.of(StandardCharsets.UTF_8, Charset.forName("ISO-8859-1"))) {
              try {
                  return tryParse(bytes, charset);
              } catch (Exception ignored) {}
          }

          throw new BusinessRuleException(
              "Nao foi possivel reconhecer o formato do arquivo CSV. " +
              "Por favor, utilize o template padrao disponivel para download."
          );
      }

      private List<ParsedTransaction> tryParse(byte[] bytes, Charset charset) throws Exception {
          BufferedReader reader = new BufferedReader(
              new InputStreamReader(new java.io.ByteArrayInputStream(bytes), charset));

          // Auto-detect separador: tenta ; depois ,
          String firstLine = reader.readLine();
          if (firstLine == null) throw new IllegalArgumentException("Arquivo vazio");

          char separator = firstLine.contains(";") ? ';' : ',';

          // Reinicia o reader
          reader = new BufferedReader(
              new InputStreamReader(new java.io.ByteArrayInputStream(bytes), charset));

          CSVFormat format = CSVFormat.DEFAULT.builder()
              .setDelimiter(separator)
              .setHeader()
              .setSkipHeaderRecord(true)
              .setIgnoreHeaderCase(true)
              .setTrim(true)
              .build();

          List<ParsedTransaction> result = new ArrayList<>();
          try (CSVParser parser = CSVParser.parse(reader, format)) {
              // Valida que tem as colunas esperadas
              var headers = parser.getHeaderNames().stream()
                  .map(String::toLowerCase).toList();
              boolean hasRequiredColumns = headers.contains("data") &&
                  headers.contains("descricao") && headers.contains("valor");
              if (!hasRequiredColumns) {
                  throw new IllegalArgumentException("Colunas obrigatorias nao encontradas");
              }

              for (CSVRecord record : parser) {
                  String rawDate = record.get("data").trim();
                  String description = record.get("descricao").trim();
                  String rawAmount = record.get("valor").trim()
                      .replace(",", ".");
                  String tipo = headers.contains("tipo") ?
                      record.get("tipo").trim().toUpperCase() : "DEBIT";

                  LocalDate date = LocalDate.parse(rawDate, DATE_FORMAT);
                  BigDecimal amount = new BigDecimal(rawAmount).abs();
                  String type = "CREDIT".equals(tipo) ? "CREDIT" : "DEBIT";

                  result.add(new ParsedTransaction(
                      date, description, amount, type,
                      Map.of("raw", record.toMap())
                  ));
              }
          }

          if (result.isEmpty()) {
              throw new IllegalArgumentException("Nenhuma transacao encontrada");
          }

          return result;
      }
  }
  ```

- [ ] Compilar:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/service/parser/CsvParser.java
  git commit -m "feat(parser): add CSV parser with auto-detect and template fallback"
  ```

---

## Task 8: BankImportService — upload e parsing

**Files:**
- Create: `...service/BankImportService.java` (interface)
- Create: `...service/impl/BankImportServiceImpl.java`

- [ ] Criar `service/BankImportService.java`:
  ```java
  package com.findash.service;

  import com.findash.dto.bankimport.*;
  import org.springframework.web.multipart.MultipartFile;
  import java.util.List;
  import java.util.UUID;

  public interface BankImportService {
      BankImportResponseDTO upload(UUID companyId, UUID userId, MultipartFile file);
      List<BankImportSummaryDTO> list(UUID companyId);
      BankImportResponseDTO getById(UUID companyId, UUID importId);
      BankImportItemResponseDTO updateItem(UUID companyId, UUID importId, UUID itemId, UpdateImportItemRequestDTO request);
      List<BankImportItemResponseDTO> updateItemsBatch(UUID companyId, UUID importId, BatchUpdateImportItemsRequestDTO request);
      void confirm(UUID companyId, UUID importId);
      void cancel(UUID companyId, UUID importId);
  }
  ```

- [ ] Criar `service/impl/BankImportServiceImpl.java` — método `upload`:
  ```java
  package com.findash.service.impl;

  import com.findash.dto.bankimport.*;
  import com.findash.entity.*;
  import com.findash.exception.BusinessRuleException;
  import com.findash.exception.ResourceNotFoundException;
  import com.findash.repository.*;
  import com.findash.service.BankImportService;
  import com.findash.service.parser.*;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;
  import org.springframework.web.multipart.MultipartFile;

  import java.util.*;

  @Service
  @Transactional
  public class BankImportServiceImpl implements BankImportService {

      private final BankImportRepository importRepository;
      private final BankImportItemRepository itemRepository;
      private final SupplierMatchRuleRepository matchRuleRepository;
      private final AccountRepository accountRepository;
      private final SupplierRepository supplierRepository;
      private final ClientRepository clientRepository;
      private final CategoryRepository categoryRepository;
      private final OfxParser ofxParser;
      private final CsvParser csvParser;

      public BankImportServiceImpl(BankImportRepository importRepository,
                                   BankImportItemRepository itemRepository,
                                   SupplierMatchRuleRepository matchRuleRepository,
                                   AccountRepository accountRepository,
                                   SupplierRepository supplierRepository,
                                   ClientRepository clientRepository,
                                   CategoryRepository categoryRepository,
                                   OfxParser ofxParser,
                                   CsvParser csvParser) {
          this.importRepository = importRepository;
          this.itemRepository = itemRepository;
          this.matchRuleRepository = matchRuleRepository;
          this.accountRepository = accountRepository;
          this.supplierRepository = supplierRepository;
          this.clientRepository = clientRepository;
          this.categoryRepository = categoryRepository;
          this.ofxParser = ofxParser;
          this.csvParser = csvParser;
      }

      @Override
      public BankImportResponseDTO upload(UUID companyId, UUID userId, MultipartFile file) {
          String filename = file.getOriginalFilename() != null ?
              file.getOriginalFilename() : "extrato";
          BankImportFileType fileType = detectFileType(filename);

          List<ParsedTransaction> transactions;
          try {
              BankStatementParser parser = fileType == BankImportFileType.OFX ? ofxParser : csvParser;
              transactions = parser.parse(file.getInputStream(), filename);
          } catch (BusinessRuleException e) {
              throw e;
          } catch (Exception e) {
              throw new BusinessRuleException("Erro ao processar arquivo: " + e.getMessage());
          }

          if (transactions.isEmpty()) {
              throw new BusinessRuleException("Nenhuma transacao encontrada no arquivo.");
          }

          BankImport bankImport = new BankImport(companyId, filename, fileType, userId);
          bankImport = importRepository.save(bankImport);

          List<SupplierMatchRule> rules = matchRuleRepository.findByCompanyId(companyId);
          List<BankImportItem> items = new ArrayList<>();

          for (ParsedTransaction tx : transactions) {
              BankImportItemType itemType = "CREDIT".equals(tx.type()) ?
                  BankImportItemType.CREDIT : BankImportItemType.DEBIT;
              AccountType accountType = itemType == BankImportItemType.DEBIT ?
                  AccountType.PAYABLE : AccountType.RECEIVABLE;

              BankImportItem item = new BankImportItem(
                  bankImport.getId(), tx.date(), tx.description(),
                  tx.amount(), itemType, accountType
              );
              item.setOriginalData(tx.rawData());

              // Deteccao de duplicado
              boolean isDuplicate = accountRepository.existsByCompanyIdAndDueDateAndAmountAndDescription(
                  companyId, tx.date(), tx.amount(), tx.description());
              item.setPossibleDuplicate(isDuplicate);

              // Sugestao por regras de matching
              applyMatchingRules(item, rules, tx.description());

              items.add(itemRepository.save(item));
          }

          bankImport.setTotalRecords(items.size());
          bankImport = importRepository.save(bankImport);

          return toResponseDTO(bankImport, items, companyId);
      }

      private BankImportFileType detectFileType(String filename) {
          String lower = filename.toLowerCase();
          if (lower.endsWith(".ofx") || lower.endsWith(".qfx")) return BankImportFileType.OFX;
          if (lower.endsWith(".csv")) return BankImportFileType.CSV;
          throw new BusinessRuleException("Formato de arquivo nao suportado. Use OFX ou CSV.");
      }

      private void applyMatchingRules(BankImportItem item, List<SupplierMatchRule> rules, String description) {
          String descLower = description.toLowerCase();
          for (SupplierMatchRule rule : rules) {
              if (descLower.contains(rule.getPattern().toLowerCase())) {
                  item.setSupplierId(rule.getSupplierId());
                  if (rule.getCategoryId() != null) item.setCategoryId(rule.getCategoryId());
                  return;
              }
          }
          // Fallback: tenta matching por nome de fornecedor/cliente
          applyNameBasedMatching(item, description);
      }

      private void applyNameBasedMatching(BankImportItem item, String description) {
          String descLower = description.toLowerCase();
          if (item.getAccountType() == AccountType.PAYABLE) {
              supplierRepository.findByCompanyIdAndActiveTrue(item.getImportId())
                  // Nota: aqui precisamos do companyId — será passado via método separado
                  // Este método será refatorado na implementação real para receber companyId
                  .stream()
                  .filter(s -> descLower.contains(s.getName().toLowerCase()))
                  .findFirst()
                  .ifPresent(s -> item.setSupplierId(s.getId()));
          }
      }

      // Demais métodos implementados nas próximas tasks
      @Override public List<BankImportSummaryDTO> list(UUID companyId) { return List.of(); }
      @Override public BankImportResponseDTO getById(UUID companyId, UUID importId) { return null; }
      @Override public BankImportItemResponseDTO updateItem(UUID companyId, UUID importId, UUID itemId, UpdateImportItemRequestDTO request) { return null; }
      @Override public List<BankImportItemResponseDTO> updateItemsBatch(UUID companyId, UUID importId, BatchUpdateImportItemsRequestDTO request) { return List.of(); }
      @Override public void confirm(UUID companyId, UUID importId) {}
      @Override public void cancel(UUID companyId, UUID importId) {}

      private BankImportResponseDTO toResponseDTO(BankImport bankImport, List<BankImportItem> items, UUID companyId) {
          List<BankImportItemResponseDTO> itemDTOs = items.stream()
              .map(item -> toItemDTO(item, companyId))
              .toList();
          return new BankImportResponseDTO(
              bankImport.getId(), bankImport.getFileName(),
              bankImport.getFileType().name(), bankImport.getStatus().name(),
              bankImport.getTotalRecords(), bankImport.getCreatedAt(), itemDTOs
          );
      }

      private BankImportItemResponseDTO toItemDTO(BankImportItem item, UUID companyId) {
          String supplierName = null;
          if (item.getSupplierId() != null) {
              supplierName = supplierRepository.findById(item.getSupplierId())
                  .map(s -> s.getName()).orElse(null);
          }
          String categoryName = null;
          if (item.getCategoryId() != null) {
              categoryName = categoryRepository.findById(item.getCategoryId())
                  .map(c -> c.getName()).orElse(null);
          }
          return new BankImportItemResponseDTO(
              item.getId(), item.getDate(), item.getDescription(), item.getAmount(),
              item.getType().name(), item.getAccountType().name(),
              item.getSupplierId(), supplierName,
              item.getCategoryId(), categoryName,
              item.isPossibleDuplicate()
          );
      }
  }
  ```

- [ ] Adicionar método de detecção de duplicado no `AccountRepository`:
  ```java
  // Em AccountRepository.java, adicionar:
  boolean existsByCompanyIdAndDueDateAndAmountAndDescription(
      UUID companyId, java.time.LocalDate dueDate,
      java.math.BigDecimal amount, String description);
  ```

- [ ] Refatorar `applyNameBasedMatching` para receber `companyId`:
  Substituir o corpo do método por:
  ```java
  private void applyNameBasedMatching(BankImportItem item, String description, UUID companyId) {
      String descLower = description.toLowerCase();
      if (item.getAccountType() == AccountType.PAYABLE) {
          supplierRepository.findByCompanyIdAndActiveTrue(companyId).stream()
              .filter(s -> descLower.contains(s.getName().toLowerCase()))
              .findFirst()
              .ifPresent(s -> item.setSupplierId(s.getId()));
      } else {
          clientRepository.findByCompanyIdAndActiveTrue(companyId).stream()
              .filter(c -> descLower.contains(c.getName().toLowerCase()))
              .findFirst()
              .ifPresent(c -> item.setSupplierId(c.getId()));
      }
  }
  ```
  E atualizar a chamada em `upload()` para `applyNameBasedMatching(item, tx.description(), companyId)`.

- [ ] Compilar:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/service/
  git add gestao-empresarial-backend/src/main/java/com/findash/repository/AccountRepository.java
  git commit -m "feat(service): add BankImportService upload and parsing logic"
  ```

---

## Task 9: BankImportService — edição de itens

**Files:**
- Modify: `...service/impl/BankImportServiceImpl.java`

- [ ] Implementar `getById` e `list`:
  ```java
  @Override
  @Transactional(readOnly = true)
  public List<BankImportSummaryDTO> list(UUID companyId) {
      return importRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
          .map(i -> new BankImportSummaryDTO(
              i.getId(), i.getFileName(), i.getFileType().name(),
              i.getStatus().name(), i.getTotalRecords(), i.getCreatedAt()))
          .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public BankImportResponseDTO getById(UUID companyId, UUID importId) {
      BankImport bankImport = findImportOrThrow(companyId, importId);
      List<BankImportItem> items = itemRepository.findByImportId(importId);
      return toResponseDTO(bankImport, items, companyId);
  }
  ```

- [ ] Implementar `updateItem`:
  ```java
  @Override
  public BankImportItemResponseDTO updateItem(UUID companyId, UUID importId,
                                               UUID itemId, UpdateImportItemRequestDTO request) {
      BankImport bankImport = findImportOrThrow(companyId, importId);
      assertEditable(bankImport);

      BankImportItem item = itemRepository.findByIdAndImportId(itemId, importId)
          .orElseThrow(() -> new ResourceNotFoundException("Item de importacao", itemId));

      if (request.supplierId() != null) item.setSupplierId(request.supplierId());
      if (request.categoryId() != null) item.setCategoryId(request.categoryId());
      if (request.accountType() != null) item.setAccountType(AccountType.valueOf(request.accountType()));

      item = itemRepository.save(item);
      return toItemDTO(item, companyId);
  }
  ```

- [ ] Implementar `updateItemsBatch`:
  ```java
  @Override
  public List<BankImportItemResponseDTO> updateItemsBatch(UUID companyId, UUID importId,
                                                           BatchUpdateImportItemsRequestDTO request) {
      BankImport bankImport = findImportOrThrow(companyId, importId);
      assertEditable(bankImport);

      List<BankImportItemResponseDTO> result = new ArrayList<>();
      for (UUID itemId : request.itemIds()) {
          itemRepository.findByIdAndImportId(itemId, importId).ifPresent(item -> {
              if (request.supplierId() != null) item.setSupplierId(request.supplierId());
              if (request.categoryId() != null) item.setCategoryId(request.categoryId());
              if (request.accountType() != null) item.setAccountType(AccountType.valueOf(request.accountType()));
              itemRepository.save(item);
              result.add(toItemDTO(item, companyId));
          });
      }
      return result;
  }
  ```

- [ ] Adicionar helper `assertEditable` e `findImportOrThrow`:
  ```java
  private BankImport findImportOrThrow(UUID companyId, UUID importId) {
      return importRepository.findByIdAndCompanyId(importId, companyId)
          .orElseThrow(() -> new ResourceNotFoundException("Importacao", importId));
  }

  private void assertEditable(BankImport bankImport) {
      if (bankImport.getStatus() != BankImportStatus.PENDING_REVIEW) {
          throw new BusinessRuleException("Esta importacao nao pode ser editada pois ja foi confirmada ou cancelada.");
      }
  }
  ```

- [ ] Compilar e rodar os testes existentes:
  ```bash
  cd gestao-empresarial-backend && ./mvnw test -q
  ```
  Esperado: `BUILD SUCCESS`

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/service/impl/BankImportServiceImpl.java
  git commit -m "feat(service): implement BankImportService list, getById, updateItem, updateItemsBatch"
  ```

---

## Task 10: BankImportService — confirmar importação

**Files:**
- Modify: `...service/impl/BankImportServiceImpl.java`

- [ ] Implementar `confirm`:
  ```java
  @Override
  public void confirm(UUID companyId, UUID importId) {
      BankImport bankImport = findImportOrThrow(companyId, importId);
      assertEditable(bankImport);

      List<BankImportItem> items = itemRepository.findByImportId(importId);

      long incomplete = items.stream()
          .filter(i -> i.getSupplierId() == null || i.getCategoryId() == null)
          .count();
      if (incomplete > 0) {
          throw new BusinessRuleException(
              incomplete + " item(ns) sem fornecedor ou categoria. Preencha todos antes de confirmar.");
      }

      for (BankImportItem item : items) {
          Account account = new Account(
              companyId, item.getAccountType(),
              item.getDescription(), item.getAmount(),
              item.getDate(), item.getCategoryId()
          );
          if (item.getAccountType() == AccountType.PAYABLE) {
              account.setSupplierId(item.getSupplierId());
          } else {
              account.setClientId(item.getSupplierId()); // supplierId guarda o clientId para RECEIVABLE
          }
          accountRepository.save(account);

          // Upsert da regra de matching
          String pattern = normalizePattern(item.getDescription());
          matchRuleRepository.findByCompanyIdAndPattern(companyId, pattern)
              .ifPresentOrElse(
                  rule -> { rule.setSupplierId(item.getSupplierId()); rule.setCategoryId(item.getCategoryId()); matchRuleRepository.save(rule); },
                  () -> matchRuleRepository.save(new SupplierMatchRule(companyId, pattern, item.getSupplierId(), item.getCategoryId()))
              );
      }

      bankImport.setStatus(BankImportStatus.COMPLETED);
      importRepository.save(bankImport);
  }

  private String normalizePattern(String description) {
      // Usa as primeiras 3 palavras como padrão
      String[] words = description.trim().split("\\s+");
      return String.join(" ", Arrays.copyOf(words, Math.min(3, words.length))).toLowerCase();
  }
  ```

- [ ] Compilar:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/service/impl/BankImportServiceImpl.java
  git commit -m "feat(service): implement BankImportService confirm with account creation and match rules"
  ```

---

## Task 11: BankImportService — cancelar

**Files:**
- Modify: `...service/impl/BankImportServiceImpl.java`

- [ ] Implementar `cancel`:
  ```java
  @Override
  public void cancel(UUID companyId, UUID importId) {
      BankImport bankImport = findImportOrThrow(companyId, importId);
      assertEditable(bankImport);
      bankImport.setStatus(BankImportStatus.CANCELLED);
      importRepository.save(bankImport);
      // items são removidos via cascade ON DELETE CASCADE no banco
      itemRepository.deleteAll(itemRepository.findByImportId(importId));
  }
  ```

- [ ] Rodar todos os testes:
  ```bash
  cd gestao-empresarial-backend && ./mvnw test -q
  ```
  Esperado: `BUILD SUCCESS`, todos os testes verdes

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/service/impl/BankImportServiceImpl.java
  git commit -m "feat(service): implement BankImportService cancel"
  ```

---

## Task 12: BankImportController

**Files:**
- Create: `...controller/BankImportController.java`

- [ ] Criar `controller/BankImportController.java`:
  ```java
  package com.findash.controller;

  import com.findash.dto.bankimport.*;
  import com.findash.security.CompanyContextHolder;
  import com.findash.service.BankImportService;
  import org.springframework.http.HttpStatus;
  import org.springframework.http.ResponseEntity;
  import org.springframework.web.bind.annotation.*;
  import org.springframework.web.multipart.MultipartFile;

  import java.util.List;
  import java.util.UUID;

  @RestController
  @RequestMapping("/api/imports")
  public class BankImportController {

      private final BankImportService bankImportService;

      public BankImportController(BankImportService bankImportService) {
          this.bankImportService = bankImportService;
      }

      @PostMapping("/upload")
      public ResponseEntity<BankImportResponseDTO> upload(
              @RequestParam("file") MultipartFile file) {
          UUID companyId = CompanyContextHolder.getCompanyId();
          UUID userId = CompanyContextHolder.getUserId();
          return ResponseEntity.status(HttpStatus.CREATED)
              .body(bankImportService.upload(companyId, userId, file));
      }

      @GetMapping
      public ResponseEntity<List<BankImportSummaryDTO>> list() {
          UUID companyId = CompanyContextHolder.getCompanyId();
          return ResponseEntity.ok(bankImportService.list(companyId));
      }

      @GetMapping("/{id}")
      public ResponseEntity<BankImportResponseDTO> getById(@PathVariable UUID id) {
          UUID companyId = CompanyContextHolder.getCompanyId();
          return ResponseEntity.ok(bankImportService.getById(companyId, id));
      }

      @PatchMapping("/{id}/items/{itemId}")
      public ResponseEntity<BankImportItemResponseDTO> updateItem(
              @PathVariable UUID id,
              @PathVariable UUID itemId,
              @RequestBody UpdateImportItemRequestDTO request) {
          UUID companyId = CompanyContextHolder.getCompanyId();
          return ResponseEntity.ok(bankImportService.updateItem(companyId, id, itemId, request));
      }

      @PatchMapping("/{id}/items/batch")
      public ResponseEntity<List<BankImportItemResponseDTO>> updateItemsBatch(
              @PathVariable UUID id,
              @RequestBody BatchUpdateImportItemsRequestDTO request) {
          UUID companyId = CompanyContextHolder.getCompanyId();
          return ResponseEntity.ok(bankImportService.updateItemsBatch(companyId, id, request));
      }

      @PostMapping("/{id}/confirm")
      public ResponseEntity<Void> confirm(@PathVariable UUID id) {
          UUID companyId = CompanyContextHolder.getCompanyId();
          bankImportService.confirm(companyId, id);
          return ResponseEntity.noContent().build();
      }

      @PostMapping("/{id}/cancel")
      public ResponseEntity<Void> cancel(@PathVariable UUID id) {
          UUID companyId = CompanyContextHolder.getCompanyId();
          bankImportService.cancel(companyId, id);
          return ResponseEntity.noContent().build();
      }
  }
  ```

- [ ] Verificar se `CompanyContextHolder` tem método `getUserId()`. Se não tiver, adicionar:
  ```java
  // Em CompanyContextHolder.java:
  public static UUID getUserId() {
      return (UUID) RequestContextHolder.currentRequestAttributes()
          .getAttribute("userId", RequestAttributes.SCOPE_REQUEST);
  }
  ```
  Se o padrão do projeto for diferente, adaptar conforme o existente.

- [ ] Compilar e rodar testes:
  ```bash
  cd gestao-empresarial-backend && ./mvnw test -q
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/controller/BankImportController.java
  git commit -m "feat(controller): add BankImportController with all import endpoints"
  ```

---

## Task 13: Testes do BankImportService

**Files:**
- Create: `...test/java/com/findash/service/impl/BankImportServiceImplTest.java`
- Create: `...test/java/com/findash/service/parser/OfxParserTest.java`
- Create: `...test/java/com/findash/service/parser/CsvParserTest.java`
- Create: `src/test/resources/samples/sample.ofx` (arquivo OFX de teste)
- Create: `src/test/resources/samples/sample.csv` (arquivo CSV de teste)

- [ ] Criar arquivo OFX de teste em `src/test/resources/samples/sample.ofx`:
  ```
  OFXHEADER:100
  DATA:OFXSGML
  VERSION:102
  SECURITY:NONE
  ENCODING:USASCII
  CHARSET:1252
  COMPRESSION:NONE
  OLDFILEUID:NONE
  NEWFILEUID:NONE

  <OFX>
  <SIGNONMSGSRSV1>
  <SONRS><STATUS><CODE>0<SEVERITY>INFO</STATUS><DTSERVER>20260115120000</DTSERVER><LANGUAGE>POR</LANGUAGE></SONRS>
  </SIGNONMSGSRSV1>
  <BANKMSGSRSV1>
  <STMTTRNRS>
  <TRNUID>1</TRNUID>
  <STMTRS>
  <CURDEF>BRL</CURDEF>
  <BANKACCTFROM><BANKID>001</BANKID><ACCTID>12345</ACCTID><ACCTTYPE>CHECKING</ACCTTYPE></BANKACCTFROM>
  <BANKTRANLIST>
  <DTSTART>20260101</DTSTART>
  <DTEND>20260131</DTEND>
  <STMTTRN>
  <TRNTYPE>DEBIT</TRNTYPE>
  <DTPOSTED>20260115120000</DTPOSTED>
  <TRNAMT>-1500.00</TRNAMT>
  <FITID>001</FITID>
  <MEMO>Pagamento Fornecedor XPTO</MEMO>
  </STMTTRN>
  <STMTTRN>
  <TRNTYPE>CREDIT</TRNTYPE>
  <DTPOSTED>20260116120000</DTPOSTED>
  <TRNAMT>3000.00</TRNAMT>
  <FITID>002</FITID>
  <MEMO>Recebimento Cliente ABC</MEMO>
  </STMTTRN>
  </BANKTRANLIST>
  <LEDGERBAL><BALAMT>1500.00</BALAMT><DTASOF>20260131</DTASOF></LEDGERBAL>
  </STMTRS>
  </STMTTRNRS>
  </BANKMSGSRSV1>
  </OFX>
  ```

- [ ] Criar arquivo CSV de teste em `src/test/resources/samples/sample.csv`:
  ```
  data,descricao,valor,tipo
  2026-01-15,Pagamento Fornecedor XPTO,1500.00,DEBIT
  2026-01-16,Recebimento Cliente ABC,3000.00,CREDIT
  ```

- [ ] Criar `OfxParserTest.java`:
  ```java
  package com.findash.service.parser;

  import org.junit.jupiter.api.Test;
  import java.io.InputStream;
  import java.util.List;
  import static org.junit.jupiter.api.Assertions.*;

  class OfxParserTest {

      private final OfxParser parser = new OfxParser();

      @Test
      void parse_validOfx_returnsTwoTransactions() throws Exception {
          InputStream input = getClass().getResourceAsStream("/samples/sample.ofx");
          List<ParsedTransaction> result = parser.parse(input, "sample.ofx");
          assertEquals(2, result.size());
      }

      @Test
      void parse_validOfx_debitIsNegativeAmount() throws Exception {
          InputStream input = getClass().getResourceAsStream("/samples/sample.ofx");
          List<ParsedTransaction> result = parser.parse(input, "sample.ofx");
          ParsedTransaction debit = result.stream().filter(t -> "DEBIT".equals(t.type())).findFirst().orElseThrow();
          assertEquals("DEBIT", debit.type());
          assertTrue(debit.amount().compareTo(java.math.BigDecimal.ZERO) > 0);
      }

      @Test
      void parse_validOfx_creditTransaction() throws Exception {
          InputStream input = getClass().getResourceAsStream("/samples/sample.ofx");
          List<ParsedTransaction> result = parser.parse(input, "sample.ofx");
          ParsedTransaction credit = result.stream().filter(t -> "CREDIT".equals(t.type())).findFirst().orElseThrow();
          assertEquals("CREDIT", credit.type());
      }
  }
  ```

- [ ] Criar `CsvParserTest.java`:
  ```java
  package com.findash.service.parser;

  import com.findash.exception.BusinessRuleException;
  import org.junit.jupiter.api.Test;
  import java.io.ByteArrayInputStream;
  import java.io.InputStream;
  import java.nio.charset.StandardCharsets;
  import java.util.List;
  import static org.junit.jupiter.api.Assertions.*;

  class CsvParserTest {

      private final CsvParser parser = new CsvParser();

      @Test
      void parse_validCsv_returnsTwoTransactions() throws Exception {
          InputStream input = getClass().getResourceAsStream("/samples/sample.csv");
          List<ParsedTransaction> result = parser.parse(input, "sample.csv");
          assertEquals(2, result.size());
      }

      @Test
      void parse_csvWithSemicolonSeparator_parsesCorrectly() throws Exception {
          String csv = "data;descricao;valor;tipo\n2026-01-15;Teste;500.00;DEBIT\n";
          InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
          List<ParsedTransaction> result = parser.parse(input, "test.csv");
          assertEquals(1, result.size());
          assertEquals("Teste", result.get(0).description());
      }

      @Test
      void parse_unrecognizedFormat_throwsBusinessRuleException() {
          String csv = "coluna1;coluna2\nvalor1;valor2\n";
          InputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
          assertThrows(BusinessRuleException.class, () -> parser.parse(input, "test.csv"));
      }
  }
  ```

- [ ] Criar `BankImportServiceImplTest.java`:
  ```java
  package com.findash.service.impl;

  import com.findash.dto.bankimport.*;
  import com.findash.entity.*;
  import com.findash.exception.BusinessRuleException;
  import com.findash.repository.*;
  import com.findash.service.parser.*;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.mock.web.MockMultipartFile;

  import java.math.BigDecimal;
  import java.time.LocalDate;
  import java.util.*;

  import static org.junit.jupiter.api.Assertions.*;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.Mockito.*;

  @ExtendWith(MockitoExtension.class)
  class BankImportServiceImplTest {

      @Mock private BankImportRepository importRepository;
      @Mock private BankImportItemRepository itemRepository;
      @Mock private SupplierMatchRuleRepository matchRuleRepository;
      @Mock private AccountRepository accountRepository;
      @Mock private SupplierRepository supplierRepository;
      @Mock private ClientRepository clientRepository;
      @Mock private CategoryRepository categoryRepository;
      @Mock private OfxParser ofxParser;
      @Mock private CsvParser csvParser;

      private BankImportServiceImpl service;
      private UUID companyId;
      private UUID userId;

      @BeforeEach
      void setUp() {
          service = new BankImportServiceImpl(importRepository, itemRepository,
              matchRuleRepository, accountRepository, supplierRepository,
              clientRepository, categoryRepository, ofxParser, csvParser);
          companyId = UUID.randomUUID();
          userId = UUID.randomUUID();
      }

      @Test
      void upload_validOfx_createsImportAndItems() throws Exception {
          var file = new MockMultipartFile("file", "extrato.ofx", "application/octet-stream",
              "dummy".getBytes());
          var tx = new ParsedTransaction(LocalDate.now(), "XPTO", new BigDecimal("100"), "DEBIT", Map.of());

          when(ofxParser.parse(any(), any())).thenReturn(List.of(tx));
          when(matchRuleRepository.findByCompanyId(companyId)).thenReturn(List.of());
          when(supplierRepository.findByCompanyIdAndActiveTrue(companyId)).thenReturn(List.of());
          when(accountRepository.existsByCompanyIdAndDueDateAndAmountAndDescription(any(), any(), any(), any()))
              .thenReturn(false);

          BankImport savedImport = new BankImport(companyId, "extrato.ofx", BankImportFileType.OFX, userId);
          savedImport.setId(UUID.randomUUID());
          when(importRepository.save(any())).thenReturn(savedImport);

          BankImportItem savedItem = new BankImportItem(savedImport.getId(), LocalDate.now(),
              "XPTO", new BigDecimal("100"), BankImportItemType.DEBIT, AccountType.PAYABLE);
          savedItem.setId(UUID.randomUUID());
          when(itemRepository.save(any())).thenReturn(savedItem);

          BankImportResponseDTO result = service.upload(companyId, userId, file);
          assertNotNull(result);
          verify(importRepository, times(2)).save(any());
          verify(itemRepository, times(1)).save(any());
      }

      @Test
      void upload_emptyFile_throws() throws Exception {
          var file = new MockMultipartFile("file", "extrato.ofx", "application/octet-stream", "dummy".getBytes());
          when(ofxParser.parse(any(), any())).thenReturn(List.of());
          when(matchRuleRepository.findByCompanyId(companyId)).thenReturn(List.of());
          assertThrows(BusinessRuleException.class, () -> service.upload(companyId, userId, file));
      }

      @Test
      void upload_unsupportedExtension_throws() {
          var file = new MockMultipartFile("file", "extrato.xls", "application/octet-stream", "dummy".getBytes());
          assertThrows(BusinessRuleException.class, () -> service.upload(companyId, userId, file));
      }

      @Test
      void confirm_withIncompleteItems_throws() {
          UUID importId = UUID.randomUUID();
          BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
          bankImport.setId(importId);

          BankImportItem item = new BankImportItem(importId, LocalDate.now(), "X",
              BigDecimal.TEN, BankImportItemType.DEBIT, AccountType.PAYABLE);
          // sem supplierId e categoryId

          when(importRepository.findByIdAndCompanyId(importId, companyId))
              .thenReturn(Optional.of(bankImport));
          when(itemRepository.findByImportId(importId)).thenReturn(List.of(item));

          assertThrows(BusinessRuleException.class, () -> service.confirm(companyId, importId));
      }

      @Test
      void cancel_pendingImport_setsStatusCancelled() {
          UUID importId = UUID.randomUUID();
          BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
          bankImport.setId(importId);

          when(importRepository.findByIdAndCompanyId(importId, companyId))
              .thenReturn(Optional.of(bankImport));
          when(itemRepository.findByImportId(importId)).thenReturn(List.of());

          service.cancel(companyId, importId);

          assertEquals(BankImportStatus.CANCELLED, bankImport.getStatus());
          verify(importRepository).save(bankImport);
      }

      @Test
      void updateItem_completedImport_throws() {
          UUID importId = UUID.randomUUID();
          BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
          bankImport.setId(importId);
          bankImport.setStatus(BankImportStatus.COMPLETED);

          when(importRepository.findByIdAndCompanyId(importId, companyId))
              .thenReturn(Optional.of(bankImport));

          assertThrows(BusinessRuleException.class,
              () -> service.updateItem(companyId, importId, UUID.randomUUID(),
                  new UpdateImportItemRequestDTO(null, null, null)));
      }
  }
  ```

- [ ] Rodar os testes:
  ```bash
  cd gestao-empresarial-backend && ./mvnw test -q
  ```
  Esperado: `BUILD SUCCESS`, todos verdes

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/test/
  git commit -m "test(bank-import): add unit tests for parsers and BankImportService"
  ```

---

## Task 14: Frontend — models e BankImportService

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/import/models/bank-import.model.ts`
- Create: `gestao-empresaial-frontend/src/app/features/import/services/bank-import.service.ts`

- [ ] Criar `models/bank-import.model.ts`:
  ```typescript
  export interface BankImportItem {
    id: string;
    date: string;
    description: string;
    amount: number;
    type: 'CREDIT' | 'DEBIT';
    accountType: 'PAYABLE' | 'RECEIVABLE';
    supplierId: string | null;
    supplierName: string | null;
    categoryId: string | null;
    categoryName: string | null;
    possibleDuplicate: boolean;
  }

  export interface BankImport {
    id: string;
    fileName: string;
    fileType: 'OFX' | 'CSV';
    status: 'PENDING_REVIEW' | 'COMPLETED' | 'CANCELLED';
    totalRecords: number;
    createdAt: string;
    items: BankImportItem[];
  }

  export interface BankImportSummary {
    id: string;
    fileName: string;
    fileType: 'OFX' | 'CSV';
    status: 'PENDING_REVIEW' | 'COMPLETED' | 'CANCELLED';
    totalRecords: number;
    createdAt: string;
  }

  export interface UpdateImportItemRequest {
    supplierId?: string;
    categoryId?: string;
    accountType?: 'PAYABLE' | 'RECEIVABLE';
  }

  export interface BatchUpdateImportItemsRequest {
    itemIds: string[];
    supplierId?: string;
    categoryId?: string;
    accountType?: 'PAYABLE' | 'RECEIVABLE';
  }
  ```

- [ ] Criar `services/bank-import.service.ts`:
  ```typescript
  import { inject, Injectable } from '@angular/core';
  import { HttpClient } from '@angular/common/http';
  import { Observable } from 'rxjs';
  import {
    BankImport,
    BankImportItem,
    BankImportSummary,
    BatchUpdateImportItemsRequest,
    UpdateImportItemRequest,
  } from '../models/bank-import.model';

  @Injectable({ providedIn: 'root' })
  export class BankImportService {
    private readonly http = inject(HttpClient);
    private readonly base = '/api/imports';

    upload(file: File): Observable<BankImport> {
      const form = new FormData();
      form.append('file', file);
      return this.http.post<BankImport>(`${this.base}/upload`, form);
    }

    list(): Observable<BankImportSummary[]> {
      return this.http.get<BankImportSummary[]>(this.base);
    }

    getById(id: string): Observable<BankImport> {
      return this.http.get<BankImport>(`${this.base}/${id}`);
    }

    updateItem(importId: string, itemId: string, body: UpdateImportItemRequest): Observable<BankImportItem> {
      return this.http.patch<BankImportItem>(`${this.base}/${importId}/items/${itemId}`, body);
    }

    updateItemsBatch(importId: string, body: BatchUpdateImportItemsRequest): Observable<BankImportItem[]> {
      return this.http.patch<BankImportItem[]>(`${this.base}/${importId}/items/batch`, body);
    }

    confirm(importId: string): Observable<void> {
      return this.http.post<void>(`${this.base}/${importId}/confirm`, {});
    }

    cancel(importId: string): Observable<void> {
      return this.http.post<void>(`${this.base}/${importId}/cancel`, {});
    }
  }
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/import/
  git commit -m "feat(frontend): add bank import models and BankImportService"
  ```

---

## Task 15: Frontend — rotas e navegação

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/import/import.routes.ts`
- Modify: `gestao-empresaial-frontend/src/app/app.routes.ts`
- Modify: sidebar component (adicionar item de navegação)

- [ ] Criar `import.routes.ts`:
  ```typescript
  import { Routes } from '@angular/router';

  export const importRoutes: Routes = [
    {
      path: '',
      loadComponent: () =>
        import('./pages/bank-import-list/bank-import-list.component').then(
          (m) => m.BankImportListComponent,
        ),
    },
    {
      path: 'nova',
      loadComponent: () =>
        import('./pages/bank-import-upload/bank-import-upload.component').then(
          (m) => m.BankImportUploadComponent,
        ),
    },
    {
      path: ':id/revisao',
      loadComponent: () =>
        import('./pages/bank-import-review/bank-import-review.component').then(
          (m) => m.BankImportReviewComponent,
        ),
    },
  ];
  ```

- [ ] Adicionar rota em `app.routes.ts`:
  ```typescript
  {
    path: 'importacao',
    loadChildren: () =>
      import('./features/import/import.routes').then((m) => m.importRoutes),
  },
  ```

- [ ] Adicionar item no sidebar (verificar o componente de sidebar atual e adicionar link para `/importacao` com ícone de upload — use `lucide` icon `Upload` ou similar do PrimeIcons).

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/import/import.routes.ts
  git add gestao-empresaial-frontend/src/app/app.routes.ts
  git commit -m "feat(frontend): add bank import routes and sidebar navigation"
  ```

---

## Task 16: Frontend — BankImportUploadComponent

**Files:**
- Create: `...import/pages/bank-import-upload/bank-import-upload.component.ts`
- Create: `...import/pages/bank-import-upload/bank-import-upload.component.html`

- [ ] Criar `bank-import-upload.component.ts`:
  ```typescript
  import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
  import { Router } from '@angular/router';
  import { BankImportService } from '../../services/bank-import.service';

  @Component({
    selector: 'app-bank-import-upload',
    templateUrl: './bank-import-upload.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
  })
  export class BankImportUploadComponent {
    private readonly router = inject(Router);
    private readonly importService = inject(BankImportService);

    protected readonly loading = signal(false);
    protected readonly error = signal<string | null>(null);
    protected readonly isDragging = signal(false);

    protected readonly maxSizeBytes = 5 * 1024 * 1024; // 5MB

    protected onDragOver(event: DragEvent): void {
      event.preventDefault();
      this.isDragging.set(true);
    }

    protected onDragLeave(): void {
      this.isDragging.set(false);
    }

    protected onDrop(event: DragEvent): void {
      event.preventDefault();
      this.isDragging.set(false);
      const file = event.dataTransfer?.files[0];
      if (file) this.processFile(file);
    }

    protected onFileSelected(event: Event): void {
      const input = event.target as HTMLInputElement;
      const file = input.files?.[0];
      if (file) this.processFile(file);
    }

    private processFile(file: File): void {
      this.error.set(null);

      if (file.size > this.maxSizeBytes) {
        this.error.set('Arquivo muito grande. O tamanho máximo é 5MB.');
        return;
      }

      const ext = file.name.toLowerCase().split('.').pop();
      if (!['ofx', 'qfx', 'csv'].includes(ext ?? '')) {
        this.error.set('Formato não suportado. Use arquivos OFX ou CSV.');
        return;
      }

      this.loading.set(true);
      this.importService.upload(file).subscribe({
        next: (result) => {
          this.loading.set(false);
          this.router.navigate(['/importacao', result.id, 'revisao']);
        },
        error: (err) => {
          this.loading.set(false);
          const msg = err?.error?.message ?? 'Erro ao processar o arquivo.';
          const isCsvError = msg.includes('template') || msg.includes('CSV');
          this.error.set(msg + (isCsvError ? '__CSV_ERROR__' : ''));
        },
      });
    }

    protected get isCsvError(): boolean {
      return this.error()?.includes('__CSV_ERROR__') ?? false;
    }

    protected get errorMessage(): string {
      return this.error()?.replace('__CSV_ERROR__', '') ?? '';
    }

    protected downloadTemplate(): void {
      const content = 'data,descricao,valor,tipo\n2026-01-15,Exemplo Fornecedor,1500.00,DEBIT\n';
      const blob = new Blob([content], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'template-importacao.csv';
      a.click();
      URL.revokeObjectURL(url);
    }
  }
  ```

- [ ] Criar `bank-import-upload.component.html`:
  ```html
  <div class="max-w-2xl mx-auto py-8 px-4">
    <div class="mb-6">
      <h1 class="text-2xl font-semibold text-gray-900">Importar Extrato</h1>
      <p class="text-sm text-gray-500 mt-1">Arquivos OFX ou CSV, máximo 5MB</p>
    </div>

    <!-- Dropzone -->
    <div
      class="border-2 border-dashed rounded-lg p-12 text-center transition-colors"
      [class]="isDragging() ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-gray-400'"
      (dragover)="onDragOver($event)"
      (dragleave)="onDragLeave()"
      (drop)="onDrop($event)"
    >
      @if (loading()) {
        <div class="flex flex-col items-center gap-3">
          <i class="pi pi-spin pi-spinner text-4xl text-blue-600"></i>
          <p class="text-gray-600">Processando arquivo...</p>
        </div>
      } @else {
        <div class="flex flex-col items-center gap-4">
          <i class="pi pi-upload text-4xl text-gray-400"></i>
          <div>
            <p class="text-gray-700 font-medium">Arraste o arquivo aqui</p>
            <p class="text-gray-500 text-sm mt-1">ou</p>
          </div>
          <label class="cursor-pointer bg-blue-600 text-white px-4 py-2 rounded-md text-sm font-medium hover:bg-blue-700 transition-colors">
            Selecionar arquivo
            <input type="file" class="hidden" accept=".ofx,.qfx,.csv" (change)="onFileSelected($event)" />
          </label>
          <p class="text-xs text-gray-400">OFX, QFX ou CSV — máximo 5MB</p>
        </div>
      }
    </div>

    <!-- Erro -->
    @if (error()) {
      <div class="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-red-700 text-sm">{{ errorMessage }}</p>
        @if (isCsvError) {
          <button
            (click)="downloadTemplate()"
            class="mt-2 text-sm text-blue-600 hover:underline font-medium"
          >
            Baixar template CSV padrão
          </button>
        }
      </div>
    }

    <!-- Link para template sempre visível -->
    <div class="mt-6 p-4 bg-gray-50 border border-gray-200 rounded-lg">
      <p class="text-sm text-gray-600">
        Usando CSV? Certifique-se que o arquivo segue o formato esperado.
      </p>
      <button
        (click)="downloadTemplate()"
        class="mt-1 text-sm text-blue-600 hover:underline"
      >
        Baixar template CSV
      </button>
    </div>
  </div>
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/import/pages/bank-import-upload/
  git commit -m "feat(frontend): add BankImportUploadComponent with dropzone and CSV fallback"
  ```

---

## Task 17: Frontend — BankImportReviewComponent

**Files:**
- Create: `...import/pages/bank-import-review/bank-import-review.component.ts`
- Create: `...import/pages/bank-import-review/bank-import-review.component.html`

- [ ] Criar `bank-import-review.component.ts`:
  ```typescript
  import {
    ChangeDetectionStrategy, Component, computed,
    effect, inject, signal,
  } from '@angular/core';
  import { ActivatedRoute, Router } from '@angular/router';
  import { FormsModule } from '@angular/forms';
  import { BankImportService } from '../../services/bank-import.service';
  import { BankImport, BankImportItem } from '../../models/bank-import.model';
  import { CategoryService } from '../../../categories/services/category.service';
  import { SupplierService } from '../../../suppliers/services/supplier.service';
  import { ClientService } from '../../../clients/services/client.service';

  @Component({
    selector: 'app-bank-import-review',
    templateUrl: './bank-import-review.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule],
  })
  export class BankImportReviewComponent {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly importService = inject(BankImportService);
    private readonly categoryService = inject(CategoryService);
    private readonly supplierService = inject(SupplierService);
    private readonly clientService = inject(ClientService);

    protected readonly bankImport = signal<BankImport | null>(null);
    protected readonly loading = signal(true);
    protected readonly confirming = signal(false);
    protected readonly selectedIds = signal<Set<string>>(new Set());
    protected readonly showConfirmCancel = signal(false);

    protected readonly suppliers = this.supplierService.suppliers;
    protected readonly clients = this.clientService.clients;
    protected readonly groups = this.categoryService.groups;

    protected readonly categories = computed(() =>
      this.groups().flatMap((g) => g.categories ?? []),
    );

    protected readonly readyCount = computed(() =>
      (this.bankImport()?.items ?? []).filter(
        (i) => i.supplierId && i.categoryId,
      ).length,
    );

    protected readonly totalCount = computed(
      () => this.bankImport()?.items?.length ?? 0,
    );

    protected readonly allReady = computed(
      () => this.readyCount() === this.totalCount() && this.totalCount() > 0,
    );

    protected readonly allSelected = computed(
      () =>
        this.selectedIds().size === this.totalCount() && this.totalCount() > 0,
    );

    constructor() {
      const id = this.route.snapshot.paramMap.get('id')!;
      this.importService.getById(id).subscribe({
        next: (data) => {
          this.bankImport.set(data);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    }

    protected toggleSelectAll(): void {
      const items = this.bankImport()?.items ?? [];
      if (this.allSelected()) {
        this.selectedIds.set(new Set());
      } else {
        this.selectedIds.set(new Set(items.map((i) => i.id)));
      }
    }

    protected toggleSelect(id: string): void {
      const next = new Set(this.selectedIds());
      next.has(id) ? next.delete(id) : next.add(id);
      this.selectedIds.set(next);
    }

    protected updateItemField(item: BankImportItem, field: keyof BankImportItem, value: string | null): void {
      const importId = this.bankImport()!.id;
      this.importService
        .updateItem(importId, item.id, { [field]: value })
        .subscribe((updated) => this.replaceItem(updated));
    }

    protected applyBulk(supplierId: string | null, categoryId: string | null): void {
      const importId = this.bankImport()!.id;
      const itemIds = Array.from(this.selectedIds());
      if (!itemIds.length) return;

      this.importService
        .updateItemsBatch(importId, { itemIds, supplierId: supplierId ?? undefined, categoryId: categoryId ?? undefined })
        .subscribe((updated) => updated.forEach((u) => this.replaceItem(u)));
    }

    private replaceItem(updated: BankImportItem): void {
      const current = this.bankImport();
      if (!current) return;
      this.bankImport.set({
        ...current,
        items: current.items.map((i) => (i.id === updated.id ? updated : i)),
      });
    }

    protected confirm(): void {
      const importId = this.bankImport()!.id;
      this.confirming.set(true);
      this.importService.confirm(importId).subscribe({
        next: () => this.router.navigate(['/importacao']),
        error: () => this.confirming.set(false),
      });
    }

    protected cancelImport(): void {
      const importId = this.bankImport()!.id;
      this.importService.cancel(importId).subscribe({
        next: () => this.router.navigate(['/importacao']),
      });
    }
  }
  ```

- [ ] Criar `bank-import-review.component.html`:
  ```html
  @if (loading()) {
    <div class="flex justify-center py-16">
      <i class="pi pi-spin pi-spinner text-3xl text-blue-600"></i>
    </div>
  } @else if (bankImport()) {
    <div class="px-6 py-6 max-w-screen-xl mx-auto">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-semibold text-gray-900">Revisão de Importação</h1>
          <p class="text-sm text-gray-500 mt-1">{{ bankImport()!.fileName }}</p>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-sm text-gray-600">
            {{ readyCount() }} de {{ totalCount() }} prontos
          </span>
          <button
            (click)="showConfirmCancel.set(true)"
            class="px-4 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50"
          >
            Cancelar
          </button>
          <button
            (click)="confirm()"
            [disabled]="!allReady() || confirming()"
            class="px-4 py-2 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            [title]="!allReady() ? 'Preencha fornecedor e categoria em todos os itens' : ''"
          >
            @if (confirming()) {
              <i class="pi pi-spin pi-spinner mr-2"></i>
            }
            Confirmar Importação
          </button>
        </div>
      </div>

      <!-- Bulk action bar -->
      @if (selectedIds().size > 0) {
        <div class="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg flex items-center gap-4">
          <span class="text-sm font-medium text-blue-700">{{ selectedIds().size }} selecionados</span>
          <select class="text-sm border border-gray-300 rounded px-2 py-1"
                  #bulkSupplier>
            <option value="">Fornecedor/Cliente...</option>
            @for (s of suppliers(); track s.id) {
              <option [value]="s.id">{{ s.name }}</option>
            }
          </select>
          <select class="text-sm border border-gray-300 rounded px-2 py-1"
                  #bulkCategory>
            <option value="">Categoria...</option>
            @for (c of categories(); track c.id) {
              <option [value]="c.id">{{ c.name }}</option>
            }
          </select>
          <button
            (click)="applyBulk(bulkSupplier.value || null, bulkCategory.value || null)"
            class="text-sm bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700"
          >
            Aplicar
          </button>
        </div>
      }

      <!-- Tabela -->
      <div class="bg-white border border-gray-200 rounded-lg overflow-hidden">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-gray-600 text-xs uppercase">
            <tr>
              <th class="px-4 py-3 w-8">
                <input type="checkbox" [checked]="allSelected()" (change)="toggleSelectAll()" />
              </th>
              <th class="px-4 py-3 text-left">Data</th>
              <th class="px-4 py-3 text-left">Descrição</th>
              <th class="px-4 py-3 text-right">Valor</th>
              <th class="px-4 py-3 text-center">Tipo</th>
              <th class="px-4 py-3 text-left">Fornecedor / Cliente</th>
              <th class="px-4 py-3 text-left">Categoria</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            @for (item of bankImport()!.items; track item.id) {
              <tr [class]="item.possibleDuplicate ? 'bg-amber-50' : 'hover:bg-gray-50'">
                <td class="px-4 py-3">
                  <input type="checkbox"
                    [checked]="selectedIds().has(item.id)"
                    (change)="toggleSelect(item.id)" />
                </td>
                <td class="px-4 py-3 text-gray-700">{{ item.date }}</td>
                <td class="px-4 py-3 text-gray-900">
                  {{ item.description }}
                  @if (item.possibleDuplicate) {
                    <span class="ml-2 text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">
                      Possível duplicado
                    </span>
                  }
                </td>
                <td class="px-4 py-3 text-right font-mono"
                    [class]="item.type === 'CREDIT' ? 'text-emerald-600' : 'text-red-600'">
                  R$ {{ item.amount | number:'1.2-2' }}
                </td>
                <td class="px-4 py-3 text-center">
                  <span class="text-xs px-2 py-1 rounded-full"
                        [class]="item.accountType === 'PAYABLE'
                          ? 'bg-red-100 text-red-700'
                          : 'bg-emerald-100 text-emerald-700'">
                    {{ item.accountType === 'PAYABLE' ? 'A Pagar' : 'A Receber' }}
                  </span>
                </td>
                <td class="px-4 py-3">
                  <select class="w-full text-sm border border-gray-300 rounded px-2 py-1"
                          [class]="!item.supplierId ? 'border-red-300 bg-red-50' : ''"
                          [ngModel]="item.supplierId"
                          (ngModelChange)="updateItemField(item, 'supplierId', $event)">
                    <option [value]="null">Selecionar...</option>
                    @for (s of suppliers(); track s.id) {
                      <option [value]="s.id">{{ s.name }}</option>
                    }
                  </select>
                </td>
                <td class="px-4 py-3">
                  <select class="w-full text-sm border border-gray-300 rounded px-2 py-1"
                          [class]="!item.categoryId ? 'border-red-300 bg-red-50' : ''"
                          [ngModel]="item.categoryId"
                          (ngModelChange)="updateItemField(item, 'categoryId', $event)">
                    <option [value]="null">Selecionar...</option>
                    @for (c of categories(); track c.id) {
                      <option [value]="c.id">{{ c.name }}</option>
                    }
                  </select>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Modal cancelar -->
      @if (showConfirmCancel()) {
        <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div class="bg-white rounded-lg p-6 max-w-sm w-full shadow-xl">
            <h3 class="text-lg font-semibold text-gray-900 mb-2">Cancelar importação?</h3>
            <p class="text-gray-600 text-sm mb-6">
              Os itens desta importação serão descartados. Esta ação não pode ser desfeita.
            </p>
            <div class="flex justify-end gap-3">
              <button (click)="showConfirmCancel.set(false)"
                class="px-4 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50">
                Voltar
              </button>
              <button (click)="cancelImport()"
                class="px-4 py-2 text-sm bg-red-600 text-white rounded-md hover:bg-red-700">
                Cancelar importação
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  }
  ```

- [ ] Adicionar `DecimalPipe` nos imports do componente (necessário para `| number`):
  ```typescript
  import { DecimalPipe } from '@angular/common';
  // Adicionar em imports: [FormsModule, DecimalPipe]
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/
  git commit -m "feat(frontend): add BankImportReviewComponent with inline edit and bulk action"
  ```

---

## Task 18: Frontend — BankImportListComponent

**Files:**
- Create: `...import/pages/bank-import-list/bank-import-list.component.ts`
- Create: `...import/pages/bank-import-list/bank-import-list.component.html`

- [ ] Criar `bank-import-list.component.ts`:
  ```typescript
  import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
  import { Router, RouterLink } from '@angular/router';
  import { BankImportService } from '../../services/bank-import.service';
  import { BankImportSummary } from '../../models/bank-import.model';

  @Component({
    selector: 'app-bank-import-list',
    templateUrl: './bank-import-list.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [RouterLink],
  })
  export class BankImportListComponent implements OnInit {
    private readonly importService = inject(BankImportService);
    private readonly router = inject(Router);

    protected readonly imports = signal<BankImportSummary[]>([]);
    protected readonly loading = signal(true);

    ngOnInit(): void {
      this.importService.list().subscribe({
        next: (data) => { this.imports.set(data); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
    }

    protected statusLabel(status: string): string {
      return { PENDING_REVIEW: 'Em Revisão', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' }[status] ?? status;
    }

    protected statusClass(status: string): string {
      return {
        PENDING_REVIEW: 'bg-amber-100 text-amber-700',
        COMPLETED: 'bg-emerald-100 text-emerald-700',
        CANCELLED: 'bg-gray-100 text-gray-600',
      }[status] ?? '';
    }
  }
  ```

- [ ] Criar `bank-import-list.component.html`:
  ```html
  <div class="px-6 py-6 max-w-screen-xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-semibold text-gray-900">Importações</h1>
      <a routerLink="nova"
         class="px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700">
        Nova Importação
      </a>
    </div>

    @if (loading()) {
      <div class="flex justify-center py-16">
        <i class="pi pi-spin pi-spinner text-3xl text-blue-600"></i>
      </div>
    } @else if (imports().length === 0) {
      <div class="text-center py-16 text-gray-500">
        <i class="pi pi-upload text-4xl mb-3 block"></i>
        <p class="font-medium">Nenhuma importação ainda</p>
        <p class="text-sm mt-1">Importe um extrato OFX ou CSV para começar</p>
      </div>
    } @else {
      <div class="bg-white border border-gray-200 rounded-lg overflow-hidden">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-gray-600 text-xs uppercase">
            <tr>
              <th class="px-4 py-3 text-left">Arquivo</th>
              <th class="px-4 py-3 text-left">Tipo</th>
              <th class="px-4 py-3 text-left">Status</th>
              <th class="px-4 py-3 text-right">Registros</th>
              <th class="px-4 py-3 text-left">Data</th>
              <th class="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            @for (imp of imports(); track imp.id) {
              <tr class="hover:bg-gray-50">
                <td class="px-4 py-3 text-gray-900 font-medium">{{ imp.fileName }}</td>
                <td class="px-4 py-3 text-gray-600">{{ imp.fileType }}</td>
                <td class="px-4 py-3">
                  <span class="text-xs px-2 py-1 rounded-full {{ statusClass(imp.status) }}">
                    {{ statusLabel(imp.status) }}
                  </span>
                </td>
                <td class="px-4 py-3 text-right text-gray-700">{{ imp.totalRecords }}</td>
                <td class="px-4 py-3 text-gray-600">{{ imp.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                <td class="px-4 py-3 text-right">
                  @if (imp.status === 'PENDING_REVIEW') {
                    <a [routerLink]="[imp.id, 'revisao']"
                       class="text-blue-600 hover:underline text-sm">Ver Revisão</a>
                  } @else {
                    <a [routerLink]="[imp.id, 'revisao']"
                       class="text-gray-500 hover:underline text-sm">Ver Detalhes</a>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  </div>
  ```

- [ ] Adicionar `DatePipe` nos imports do componente:
  ```typescript
  import { DatePipe } from '@angular/common';
  // Adicionar em imports: [RouterLink, DatePipe]
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/import/pages/bank-import-list/
  git commit -m "feat(frontend): add BankImportListComponent"
  ```

---

## Task 19: Template CSV + ajustes finais

- [ ] Verificar o `CompanyContextHolder` e garantir que `getUserId()` existe (Task 12 pendente)

- [ ] Verificar que `findByCompanyIdAndActiveTrue` existe em `ClientRepository` (é chamado na sugestão de matching — fase 8). Se não existir, adicionar ao `ClientRepository`:
  ```java
  List<Client> findByCompanyIdAndActiveTrue(UUID companyId);
  ```

- [ ] Rodar todos os testes backend:
  ```bash
  cd gestao-empresarial-backend && ./mvnw test
  ```
  Esperado: todos os testes verdes

- [ ] Rodar o frontend e testar manualmente o fluxo:
  ```bash
  cd gestao-empresaial-frontend && npm start
  ```
  Testar: upload de OFX → tela de revisão → vincular fornecedor/categoria → confirmar → verificar conta criada no banco

- [ ] Commit final de ajustes:
  ```bash
  git add -A
  git commit -m "fix: final adjustments for bank import phase 5"
  ```

---

## Task 20: Push e Pull Request

- [ ] Push da branch:
  ```bash
  git push origin feature/phase5-bank-import
  ```

- [ ] Criar Pull Request para `master` via GitHub, descrevendo:
  - Upload síncrono OFX e CSV
  - Tela de revisão com edição inline e bulk
  - Detecção de duplicados (badge âmbar)
  - Sugestão automática de fornecedor
  - Confirmação gera contas a pagar/receber

---

## Notas de Implementação

### CompanyContextHolder
Verificar em `security/CompanyContextHolder.java` como o `userId` é extraído do JWT. O padrão do projeto usa `ThreadLocal` ou atributos do request. Adaptar conforme o existente.

### Supplier vs Client no item
O campo `supplierId` em `BankImportItem` é usado tanto para fornecedor (PAYABLE) quanto para cliente (RECEIVABLE). Na confirmação, o service verifica o `accountType` para decidir se cria `account.supplierId` ou `account.clientId`.

### Matching de nome (fallback)
O `applyNameBasedMatching` usa `findByCompanyIdAndActiveTrue` de ambos os repositórios. Como o `BankImportItem` não tem `companyId` direto, o `companyId` deve ser passado como parâmetro explícito — conforme refatoração indicada na Task 8.
