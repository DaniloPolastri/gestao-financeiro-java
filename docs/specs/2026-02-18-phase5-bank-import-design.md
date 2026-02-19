# Phase 5: Importação OFX/CSV — Design Spec

**Data:** 18/02/2026
**Status:** Approved

---

## Resumo

Implementar importação de extratos bancários OFX e CSV com parsing síncrono, tela de revisão com vínculo a fornecedor/categoria, detecção de duplicados, sugestão automática por regras de matching e confirmação que gera contas a pagar/receber.

---

## Decisões de Design

| Decisão | Escolha |
|---------|---------|
| Processamento | Síncrono (parse na mesma request do upload) |
| Sugestão de fornecedor | Matching por texto (1ª importação) + regras salvas (importações seguintes) |
| Duplicados | Badge de aviso na revisão, usuário decide se importa |
| CSV layout | Auto-detecção + fallback para template descrito na UI |
| UX de revisão | Edição inline + bulk action para múltiplos itens |
| Tipo de conta | Sugerido por DEBIT/CREDIT, editável pelo usuário |

---

## Data Model

### bank_imports (financial_schema)

| Coluna | Tipo | Notas |
|--------|------|-------|
| id | UUID PK | |
| company_id | UUID NOT NULL | |
| file_name | VARCHAR(255) | Nome original do arquivo |
| file_type | ENUM(OFX, CSV) | |
| status | ENUM(PENDING_REVIEW, COMPLETED, CANCELLED) | |
| total_records | INT | Total de itens parseados |
| imported_by | UUID | user_id de quem fez upload |
| created_at | TIMESTAMP | |

### bank_import_items (financial_schema)

| Coluna | Tipo | Notas |
|--------|------|-------|
| id | UUID PK | |
| import_id | UUID FK → bank_imports | |
| date | DATE | |
| description | VARCHAR(500) | Descrição original do extrato |
| amount | DECIMAL(15,2) | Sempre positivo |
| type | ENUM(CREDIT, DEBIT) | Tipo original do extrato |
| account_type | ENUM(PAYABLE, RECEIVABLE) | Sugerido, editável pelo usuário |
| supplier_id | UUID nullable FK → suppliers | |
| category_id | UUID nullable FK → categories | |
| possible_duplicate | BOOLEAN DEFAULT false | |
| original_data | JSONB | Dado bruto para auditoria |
| created_at | TIMESTAMP | |

### supplier_match_rules (financial_schema)

| Coluna | Tipo | Notas |
|--------|------|-------|
| id | UUID PK | |
| company_id | UUID NOT NULL | |
| pattern | VARCHAR(255) | Substring da descrição do extrato |
| supplier_id | UUID FK → suppliers | |
| category_id | UUID nullable FK → categories | |
| created_at | TIMESTAMP | |

**Regras de negócio:**
- `possible_duplicate` detectado por existência em `accounts` com mesmos `(company_id, date, amount, description)`
- `account_type` default: DEBIT → PAYABLE, CREDIT → RECEIVABLE
- Confirmação bloqueada se qualquer item sem `supplier_id` ou `category_id`
- Ao confirmar: cria `Account` por item + upsert em `supplier_match_rules` pelo padrão da descrição
- Import `COMPLETED` ou `CANCELLED` é imutável

---

## API Endpoints

Todos sob `/api/financial/imports`, autenticados, com header `X-Company-Id`.

| Método | Path | Descrição | Role |
|--------|------|-----------|------|
| `POST` | `/upload` | Upload + parse síncrono | EDITOR+ |
| `GET` | `/` | Histórico de importações | VIEWER+ |
| `GET` | `/{id}` | Detalhe com itens | VIEWER+ |
| `PATCH` | `/{id}/items/{itemId}` | Atualiza item individual | EDITOR+ |
| `PATCH` | `/{id}/items/batch` | Atualiza múltiplos itens | EDITOR+ |
| `POST` | `/{id}/confirm` | Confirma, gera accounts | EDITOR+ |
| `POST` | `/{id}/cancel` | Cancela, remove itens | EDITOR+ |

### Fluxo POST /upload

```
1. Valida: tamanho < 5MB, extensão OFX ou CSV
2. Detecta tipo pelo conteúdo:
   - OFX: ofx4j → parse transactions
   - CSV: auto-detect separador, encoding, colunas (date/amount/description)
     └── Fallback: retorna 422 com instrução para usar template
3. Para cada transação:
   a. Detecta duplicado: busca em accounts por (company_id, date, amount, description)
   b. Sugere account_type: DEBIT→PAYABLE, CREDIT→RECEIVABLE
   c. Aplica supplier_match_rules: contains case-insensitive no description
4. Salva bank_import + bank_import_items
5. Retorna BankImportResponseDTO com todos os itens e sugestões aplicadas
```

### POST /{id}/confirm

```
1. Valida: todos os itens têm supplier_id e category_id
2. Para cada item:
   a. Cria Account (type=account_type, supplierId ou clientId conforme type)
   b. Upsert supplier_match_rules com pattern = description (ou substring normalizada)
3. Atualiza bank_import.status = COMPLETED
```

---

## Frontend

### Rotas (lazy-loaded)

```
/importacao              → BankImportListComponent
/importacao/nova         → BankImportUploadComponent
/importacao/:id/revisao  → BankImportReviewComponent
```

### BankImportUploadComponent

- Dropzone drag-and-drop + botão de seleção de arquivo
- Validação client-side: < 5MB, extensão OFX ou CSV
- Spinner com mensagem durante upload/parse
- Link para download do template CSV (fallback)
- Erro 422 (CSV não reconhecido): mensagem clara + destaca link do template

### BankImportReviewComponent

- Tabela com todos os itens do import
- Colunas: data, descrição, valor, tipo (CREDIT/DEBIT), conta (PAYABLE/RECEIVABLE), fornecedor, categoria
- Badge âmbar em itens com `possible_duplicate: true`
- **Edição inline**: selects de fornecedor/categoria/account_type editáveis direto na célula
- **Bulk action**: checkbox por linha + toolbar flutuante "Aplicar para X selecionados"
- Contador: "X de Y itens prontos"
- Botão **Confirmar**: desabilitado enquanto houver itens incompletos, tooltip explica
- Botão **Cancelar**: modal de confirmação → navega para histórico

### BankImportListComponent

- Tabela: arquivo, data, status (badge), total de registros
- Ação: "Ver Revisão" se PENDING_REVIEW, "Ver Detalhes" se COMPLETED

### BankImportService

```typescript
upload(file: File): Observable<BankImportResponseDTO>
list(): Observable<BankImportSummaryDTO[]>
getById(id: string): Observable<BankImportResponseDTO>
updateItem(importId: string, itemId: string, patch: UpdateImportItemDTO): Observable<BankImportItemDTO>
updateItemsBatch(importId: string, patches: UpdateImportItemDTO[]): Observable<BankImportItemDTO[]>
confirm(importId: string): Observable<void>
cancel(importId: string): Observable<void>
```

---

## Dependências

### Backend (Maven)
- `com.webcohesion.ofx4j:ofx4j` — parsing OFX
- `org.apache.commons:commons-csv` — parsing CSV

### Frontend
- Sem novas dependências — PrimeNG já tem FileUpload e Table com seleção

---

## Template CSV (fallback)

Formato esperado pelo template:

```
data,descricao,valor,tipo
2026-01-15,Pagamento Fornecedor XPTO,1500.00,DEBIT
2026-01-16,Recebimento Cliente ABC,3000.00,CREDIT
```

- `data`: formato `yyyy-MM-dd`
- `tipo`: `DEBIT` ou `CREDIT`
- `valor`: ponto como separador decimal, sempre positivo

---

## Edge Cases

| Caso | Comportamento |
|------|---------------|
| Arquivo > 5MB | 400 com mensagem clara antes do upload |
| Extensão inválida | 400 client-side antes do upload |
| OFX malformado | 422 com mensagem "Arquivo OFX inválido ou corrompido" |
| CSV não reconhecido | 422 com link para template |
| Import já COMPLETED/CANCELLED | 409 se tentar editar ou confirmar |
| Confirmar com itens incompletos | 422 listando quantos itens estão faltando |
| Nenhuma transação no arquivo | 422 "Nenhuma transação encontrada no arquivo" |
