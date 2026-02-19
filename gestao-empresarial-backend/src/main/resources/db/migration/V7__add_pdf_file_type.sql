-- Allow PDF as a valid file_type for bank imports
ALTER TABLE financial_schema.bank_imports
    DROP CONSTRAINT IF EXISTS bank_imports_file_type_check;

ALTER TABLE financial_schema.bank_imports
    ADD CONSTRAINT bank_imports_file_type_check
    CHECK (file_type IN ('OFX', 'CSV', 'PDF'));
