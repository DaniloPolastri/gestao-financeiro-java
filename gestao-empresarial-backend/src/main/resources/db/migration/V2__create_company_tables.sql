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
