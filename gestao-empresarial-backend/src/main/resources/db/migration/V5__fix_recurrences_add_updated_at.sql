-- Fix: add missing updated_at column to recurrences table
ALTER TABLE financial_schema.recurrences
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
