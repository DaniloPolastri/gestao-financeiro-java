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
