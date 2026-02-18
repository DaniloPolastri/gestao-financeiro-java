-- Recurrences table
CREATE TABLE financial_schema.recurrences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    frequency VARCHAR(10) NOT NULL CHECK (frequency IN ('MONTHLY', 'WEEKLY', 'BIWEEKLY', 'YEARLY')),
    start_date DATE NOT NULL,
    end_date DATE,
    max_occurrences INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_recurrence_company FOREIGN KEY (company_id)
        REFERENCES company_schema.companies(id) ON DELETE CASCADE
);

CREATE INDEX idx_recurrence_company ON financial_schema.recurrences(company_id);

-- Accounts table (unified payable + receivable)
CREATE TABLE financial_schema.accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('PAYABLE', 'RECEIVABLE')),
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    due_date DATE NOT NULL,
    payment_date DATE,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'RECEIVED', 'OVERDUE', 'PARTIAL')),
    category_id UUID NOT NULL,
    supplier_id UUID,
    client_id UUID,
    recurrence_id UUID,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_account_company FOREIGN KEY (company_id)
        REFERENCES company_schema.companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_account_category FOREIGN KEY (category_id)
        REFERENCES financial_schema.categories(id),
    CONSTRAINT fk_account_supplier FOREIGN KEY (supplier_id)
        REFERENCES financial_schema.suppliers(id),
    CONSTRAINT fk_account_client FOREIGN KEY (client_id)
        REFERENCES financial_schema.clients(id),
    CONSTRAINT fk_account_recurrence FOREIGN KEY (recurrence_id)
        REFERENCES financial_schema.recurrences(id)
);

CREATE INDEX idx_account_company_type_status ON financial_schema.accounts(company_id, type, status);
CREATE INDEX idx_account_company_due_date ON financial_schema.accounts(company_id, due_date);
CREATE INDEX idx_account_company_supplier ON financial_schema.accounts(company_id, supplier_id);
CREATE INDEX idx_account_company_client ON financial_schema.accounts(company_id, client_id);
CREATE INDEX idx_account_category ON financial_schema.accounts(category_id);
CREATE INDEX idx_account_recurrence ON financial_schema.accounts(recurrence_id);
