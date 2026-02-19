# PDF Bank Statement Import — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add PDF bank statement import alongside existing OFX/CSV, using Apache PDFBox for text extraction and regex for transaction detection.

**Architecture:** Extends existing Strategy Pattern — new `PdfParser` implements `BankStatementParser`. Service routes `.pdf` files to the new parser. Frontend accepts `.pdf` in upload. DB migration updates CHECK constraint.

**Tech Stack:** Apache PDFBox 3.0.4, Java 17 regex, existing Spring Boot 4 + Angular 21 stack.

---

### Task 1: Add PDFBox dependency

**Files:**
- Modify: `gestao-empresarial-backend/pom.xml:95` (after commons-csv dependency)

**Step 1: Add dependency to pom.xml**

Add after the `commons-csv` dependency block (around line 102):

```xml
        <!-- PDF parsing -->
        <dependency>
            <groupId>org.apache.pdfbox</groupId>
            <artifactId>pdfbox</artifactId>
            <version>3.0.4</version>
        </dependency>
```

**Step 2: Verify it resolves**

Run: `cd gestao-empresarial-backend && ./mvnw dependency:resolve -q`
Expected: BUILD SUCCESS (no errors)

**Step 3: Commit**

```bash
git add gestao-empresarial-backend/pom.xml
git commit -m "build: add Apache PDFBox 3.0.4 dependency for PDF import"
```

---

### Task 2: Add PDF to BankImportFileType enum

**Files:**
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/entity/BankImportFileType.java`

**Step 1: Update enum**

Change:
```java
public enum BankImportFileType {
    OFX, CSV
}
```

To:
```java
public enum BankImportFileType {
    OFX, CSV, PDF
}
```

**Step 2: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/entity/BankImportFileType.java
git commit -m "feat: add PDF to BankImportFileType enum"
```

---

### Task 3: Add DB migration for PDF file type

**Files:**
- Create: `gestao-empresarial-backend/src/main/resources/db/migration/V7__add_pdf_file_type.sql`

**Step 1: Create migration**

The current `bank_imports.file_type` column uses a CHECK constraint: `CHECK (file_type IN ('OFX', 'CSV'))`. We need to drop and recreate it to include `'PDF'`.

```sql
-- Allow PDF as a valid file_type for bank imports
ALTER TABLE financial_schema.bank_imports
    DROP CONSTRAINT IF EXISTS bank_imports_file_type_check;

ALTER TABLE financial_schema.bank_imports
    ADD CONSTRAINT bank_imports_file_type_check
    CHECK (file_type IN ('OFX', 'CSV', 'PDF'));
```

**Step 2: Commit**

```bash
git add gestao-empresarial-backend/src/main/resources/db/migration/V7__add_pdf_file_type.sql
git commit -m "feat: add migration to allow PDF file type in bank_imports"
```

---

### Task 4: Create PdfParser — write failing tests first

**Files:**
- Create: `gestao-empresarial-backend/src/test/java/com/findash/service/parser/PdfParserTest.java`
- Create: `gestao-empresarial-backend/src/test/resources/samples/sample-bank-statement.pdf`

**Step 1: Create sample PDF test fixture**

Use PDFBox programmatically in a test helper to generate a minimal PDF with bank statement text. The PDF text content should look like a typical Brazilian bank statement:

```
Extrato Bancario - Banco Exemplo
Periodo: 01/01/2026 a 31/01/2026

Data        Descricao                          Valor
15/01/2026  PIX RECEBIDO - CLIENTE ABC      1.500,00 C
16/01/2026  COMPRA DEBITO - LOJA XYZ          -89,90
18/01/2026  TED ENVIADA - FORNECEDOR 123   -3.000,00
20/01/2026  DEPOSITO EM CONTA                 500,00 C
```

Create a test utility method `createSamplePdf(String textContent)` that uses PDFBox to generate a PDF in memory and returns an `InputStream`:

```java
package com.findash.service.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfParserTest {

    private final PdfParser parser = new PdfParser();

    private InputStream createSamplePdf(String... lines) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 10);
                cs.setLeading(14f);
                cs.newLineAtOffset(50, 700);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLine();
                }
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Test
    void parse_validPdfWithTransactions_returnsCorrectCount() throws Exception {
        InputStream input = createSamplePdf(
            "Extrato Bancario - Banco Exemplo",
            "Periodo: 01/01/2026 a 31/01/2026",
            "",
            "15/01/2026  PIX RECEBIDO - CLIENTE ABC      1.500,00 C",
            "16/01/2026  COMPRA DEBITO - LOJA XYZ           89,90",
            "18/01/2026  TED ENVIADA - FORNECEDOR 123    3.000,00",
            "20/01/2026  DEPOSITO EM CONTA                 500,00 C"
        );

        List<ParsedTransaction> result = parser.parse(input, "extrato.pdf");

        assertEquals(4, result.size());
    }

    @Test
    void parse_detectsCreditAndDebit_correctly() throws Exception {
        InputStream input = createSamplePdf(
            "15/01/2026  PIX RECEBIDO      1.500,00 C",
            "16/01/2026  COMPRA DEBITO        89,90"
        );

        List<ParsedTransaction> result = parser.parse(input, "extrato.pdf");

        assertEquals(2, result.size());
        assertEquals("CREDIT", result.get(0).type());
        assertEquals("DEBIT", result.get(1).type());
    }

    @Test
    void parse_negativeAmounts_detectedAsDebit() throws Exception {
        InputStream input = createSamplePdf(
            "18/01/2026  TED ENVIADA    -3.000,00"
        );

        List<ParsedTransaction> result = parser.parse(input, "extrato.pdf");

        assertEquals(1, result.size());
        assertEquals("DEBIT", result.get(0).type());
        assertEquals(new BigDecimal("3000.00"), result.get(0).amount());
    }

    @Test
    void parse_parsesAmountCorrectly_brazilianFormat() throws Exception {
        InputStream input = createSamplePdf(
            "20/01/2026  PAGAMENTO    1.234,56"
        );

        List<ParsedTransaction> result = parser.parse(input, "extrato.pdf");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("1234.56"), result.get(0).amount());
    }

    @Test
    void parse_shortDateFormat_ddMMyy() throws Exception {
        InputStream input = createSamplePdf(
            "15/01/26  TRANSFERENCIA    250,00 C"
        );

        List<ParsedTransaction> result = parser.parse(input, "extrato.pdf");

        assertEquals(1, result.size());
        assertEquals(2026, result.get(0).date().getYear());
    }

    @Test
    void parse_noTransactionsFound_throwsException() {
        assertThrows(Exception.class, () -> {
            InputStream input = createSamplePdf(
                "Este documento nao contem transacoes",
                "Apenas texto informativo"
            );
            parser.parse(input, "extrato.pdf");
        });
    }

    @Test
    void parse_amountWithRsPrefix_parsedCorrectly() throws Exception {
        InputStream input = createSamplePdf(
            "15/01/2026  PAGAMENTO BOLETO    R$ 450,00"
        );

        List<ParsedTransaction> result = parser.parse(input, "extrato.pdf");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("450.00"), result.get(0).amount());
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `cd gestao-empresarial-backend && ./mvnw test -pl . -Dtest="PdfParserTest" -q`
Expected: COMPILATION FAILURE — `PdfParser` class does not exist yet

**Step 3: Commit**

```bash
git add gestao-empresarial-backend/src/test/java/com/findash/service/parser/PdfParserTest.java
git commit -m "test: add PdfParser unit tests (red phase)"
```

---

### Task 5: Implement PdfParser

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/service/parser/PdfParser.java`

**Step 1: Implement the parser**

```java
package com.findash.service.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfParser implements BankStatementParser {

    // Date patterns: dd/MM/yyyy or dd/MM/yy
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "^\\s*(\\d{2}/\\d{2}/(?:\\d{4}|\\d{2}))\\s+"
    );

    // Amount patterns: optional minus, optional R$, digits with dots (thousands), comma (decimal)
    // Matches: 1.500,00  -89,90  R$ 450,00  -3.000,00  500,00 C  500,00 D
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "(-?)\\s*(?:R\\$\\s*)?(-?)(\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\s*([CDcd])?\\s*$"
    );

    private static final DateTimeFormatter FMT_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_SHORT = DateTimeFormatter.ofPattern("dd/MM/yy");

    @Override
    public List<ParsedTransaction> parse(InputStream input, String filename) throws Exception {
        String text;
        try (PDDocument doc = Loader.loadPDF(input.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(doc);
        }

        List<ParsedTransaction> transactions = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            ParsedTransaction tx = tryParseLine(line);
            if (tx != null) {
                transactions.add(tx);
            }
        }

        if (transactions.isEmpty()) {
            throw new IllegalArgumentException(
                "Nao foi possivel extrair transacoes deste PDF. " +
                "Tente exportar o extrato do seu banco em formato OFX ou CSV."
            );
        }

        return transactions;
    }

    private ParsedTransaction tryParseLine(String line) {
        if (line == null || line.isBlank()) return null;

        Matcher dateMatcher = DATE_PATTERN.matcher(line);
        if (!dateMatcher.find()) return null;

        String dateStr = dateMatcher.group(1);
        LocalDate date = parseDate(dateStr);
        if (date == null) return null;

        Matcher amountMatcher = AMOUNT_PATTERN.matcher(line);
        if (!amountMatcher.find()) return null;

        String minusPrefix = amountMatcher.group(1);
        String minusAfterRs = amountMatcher.group(2);
        String amountStr = amountMatcher.group(3);
        String creditDebitFlag = amountMatcher.group(4);

        // Parse amount: remove dots (thousands), replace comma with dot (decimal)
        BigDecimal amount = new BigDecimal(
            amountStr.replace(".", "").replace(",", ".")
        );

        // Determine credit/debit
        boolean isNegative = !minusPrefix.isEmpty() || !minusAfterRs.isEmpty();
        boolean hasDebitFlag = creditDebitFlag != null &&
            creditDebitFlag.equalsIgnoreCase("D");
        boolean hasCreditFlag = creditDebitFlag != null &&
            creditDebitFlag.equalsIgnoreCase("C");

        String type;
        if (hasCreditFlag) {
            type = "CREDIT";
        } else if (isNegative || hasDebitFlag) {
            type = "DEBIT";
        } else {
            type = "DEBIT"; // default when ambiguous
        }

        // Extract description: text between date and amount
        int descStart = dateMatcher.end();
        int descEnd = amountMatcher.start();
        String description = line.substring(descStart, descEnd).trim();
        if (description.isEmpty()) return null;

        return new ParsedTransaction(
            date, description, amount, type,
            Map.of("rawLine", line.trim())
        );
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, FMT_FULL);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(dateStr, FMT_SHORT);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
}
```

**Step 2: Run tests to verify they pass**

Run: `cd gestao-empresarial-backend && ./mvnw test -pl . -Dtest="PdfParserTest" -q`
Expected: All 7 tests PASS

**Step 3: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/service/parser/PdfParser.java
git commit -m "feat: implement PdfParser with regex-based transaction extraction"
```

---

### Task 6: Wire PdfParser into BankImportServiceImpl

**Files:**
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/service/impl/BankImportServiceImpl.java`

**Step 1: Add PdfParser field and constructor parameter**

Add field alongside existing parsers (line 28):
```java
    private final PdfParser pdfParser;
```

Update constructor signature (line 30-48) to include `PdfParser pdfParser` parameter and add `this.pdfParser = pdfParser;` in the body.

**Step 2: Update detectFileType method**

Change (line 218-222):
```java
    private BankImportFileType detectFileType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".ofx") || lower.endsWith(".qfx")) return BankImportFileType.OFX;
        if (lower.endsWith(".csv")) return BankImportFileType.CSV;
        throw new BusinessRuleException("Formato de arquivo nao suportado. Use OFX ou CSV.");
    }
```

To:
```java
    private BankImportFileType detectFileType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".ofx") || lower.endsWith(".qfx")) return BankImportFileType.OFX;
        if (lower.endsWith(".csv")) return BankImportFileType.CSV;
        if (lower.endsWith(".pdf")) return BankImportFileType.PDF;
        throw new BusinessRuleException("Formato de arquivo nao suportado. Use OFX, CSV ou PDF.");
    }
```

**Step 3: Update parser selection in upload method**

Change (line 58):
```java
            BankStatementParser parser = fileType == BankImportFileType.OFX ? ofxParser : csvParser;
```

To:
```java
            BankStatementParser parser = switch (fileType) {
                case OFX -> ofxParser;
                case CSV -> csvParser;
                case PDF -> pdfParser;
            };
```

**Step 4: Run all tests**

Run: `cd gestao-empresarial-backend && ./mvnw test -q`
Expected: All tests PASS

**Step 5: Commit**

```bash
git add gestao-empresarial-backend/src/main/java/com/findash/service/impl/BankImportServiceImpl.java
git commit -m "feat: wire PdfParser into import service with file type detection"
```

---

### Task 7: Update frontend model

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/import/models/bank-import.model.ts`

**Step 1: Add PDF to fileType union types**

Change all occurrences of `'OFX' | 'CSV'` to `'OFX' | 'CSV' | 'PDF'` in:
- `BankImport.fileType` (line 18)
- `BankImportSummary.fileType` (line 27)

**Step 2: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/import/models/bank-import.model.ts
git commit -m "feat: add PDF to frontend BankImport fileType model"
```

---

### Task 8: Update frontend upload component

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-upload/bank-import-upload.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-upload/bank-import-upload.component.html`

**Step 1: Update file extension validation in TS**

Change (line 51):
```typescript
    if (!['ofx', 'qfx', 'csv'].includes(ext ?? '')) {
```
To:
```typescript
    if (!['ofx', 'qfx', 'csv', 'pdf'].includes(ext ?? '')) {
```

Change error message (line 52):
```typescript
      this.error.set('Formato não suportado. Use arquivos OFX ou CSV.');
```
To:
```typescript
      this.error.set('Formato não suportado. Use arquivos OFX, CSV ou PDF.');
```

**Step 2: Update accepted file types in HTML template**

Change the file input `accept` attribute:
```html
<input type="file" class="hidden" accept=".ofx,.qfx,.csv,.pdf" (change)="onFileSelected($event)" />
```

Update the supported formats text:
```html
<p class="text-xs text-gray-400">OFX, QFX, CSV ou PDF — máximo 5MB</p>
```

**Step 3: Build frontend to verify**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds with no errors

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/import/pages/bank-import-upload/bank-import-upload.component.ts
git add gestao-empresaial-frontend/src/app/features/import/pages/bank-import-upload/bank-import-upload.component.html
git commit -m "feat: accept PDF files in upload component"
```

---

### Task 9: Run full test suite and verify

**Step 1: Backend tests**

Run: `cd gestao-empresarial-backend && ./mvnw test -q`
Expected: All tests PASS including new PdfParserTest

**Step 2: Frontend build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds

**Step 3: Final commit (if any fixups needed)**

If all green, no commit needed. Feature is complete.

---

## Summary of All Files

| # | Action | File |
|---|--------|------|
| 1 | Modify | `gestao-empresarial-backend/pom.xml` |
| 2 | Modify | `gestao-empresarial-backend/src/main/java/com/findash/entity/BankImportFileType.java` |
| 3 | Create | `gestao-empresarial-backend/src/main/resources/db/migration/V7__add_pdf_file_type.sql` |
| 4 | Create | `gestao-empresarial-backend/src/test/java/com/findash/service/parser/PdfParserTest.java` |
| 5 | Create | `gestao-empresarial-backend/src/main/java/com/findash/service/parser/PdfParser.java` |
| 6 | Modify | `gestao-empresarial-backend/src/main/java/com/findash/service/impl/BankImportServiceImpl.java` |
| 7 | Modify | `gestao-empresaial-frontend/src/app/features/import/models/bank-import.model.ts` |
| 8 | Modify | `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-upload/bank-import-upload.component.ts` |
| 8 | Modify | `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-upload/bank-import-upload.component.html` |
