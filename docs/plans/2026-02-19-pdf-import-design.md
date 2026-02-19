# PDF Bank Statement Import — Design

## Context

FinDash currently supports OFX and CSV bank statement imports via a Strategy Pattern (`BankStatementParser` interface with `OfxParser` and `CsvParser`). Users want to also import PDF bank statements, which are common in Brazilian banks.

## Approach

**Generic table extraction using Apache PDFBox.** Extract raw text from PDF, scan lines with regex to detect transactions (date + description + amount). When parsing fails, return a clear error suggesting OFX/CSV instead.

## Architecture

Extends the existing Strategy Pattern — no structural changes needed.

```
BankStatementParser (interface)
├── OfxParser      (existing)
├── CsvParser      (existing)
└── PdfParser      (new)
```

## Backend Changes

### 1. Dependency

Add `org.apache.pdfbox:pdfbox:3.0.4` to `pom.xml`.

### 2. PdfParser

New class implementing `BankStatementParser`:

- **Text extraction**: `PDDocument.load()` + `PDFTextStripper.getText()`
- **Line-by-line scan** with regex patterns:
  - Date: `dd/MM/yyyy`, `dd/MM/yy`, `dd/MM` (assumes current year)
  - Amount: Brazilian format `1.234,56` or `-1.234,56`, optional `R$` prefix
  - Description: text between date and amount
- **Credit/Debit**: negative amount or `D` suffix → DEBIT, positive or `C` suffix → CREDIT
- **Failure**: if zero transactions found, throw `IllegalArgumentException` with descriptive message

### 3. BankImportFileType Enum

Add `PDF` value.

### 4. BankImportServiceImpl

Update `detectFileType()` to map `.pdf` → `PDF`. Update parser selection to route `PDF` → `PdfParser`.

### 5. DB Migration

New Flyway migration: `ALTER TYPE` or update check constraint to allow `'PDF'` in `bank_imports.file_type`.

## Frontend Changes

### 6. Upload Component

- Add `.pdf` to `accept` attribute: `.ofx,.qfx,.csv,.pdf`
- Update extension validation to include `pdf`
- Update error message: "Use arquivos OFX, CSV ou PDF"

### 7. Model

Add `'PDF'` to `fileType` union: `'OFX' | 'CSV' | 'PDF'`

## Error Handling

When PdfParser can't extract transactions:
- Backend: `IllegalArgumentException("Não foi possível extrair transações deste PDF")`
- Frontend: displays error + suggestion to use OFX/CSV

## What Does NOT Change

- Review page (same item flow)
- Confirm/cancel flow
- DTOs (only fileType value adds PDF)
- Controller (already accepts any MultipartFile)
- Matching rules / duplicate detection

## Files Modified

| File | Change |
|------|--------|
| `pom.xml` | Add pdfbox dependency |
| `PdfParser.java` | New file — PDF parsing logic |
| `BankImportFileType.java` | Add `PDF` enum value |
| `BankImportServiceImpl.java` | Route `.pdf` to PdfParser |
| `V*.sql` | Migration for file_type enum |
| `bank-import-upload.component.ts` | Accept .pdf, update validation |
| `bank-import.model.ts` | Add 'PDF' to fileType type |
