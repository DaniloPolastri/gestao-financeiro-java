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
