CREATE TABLE auth_schema.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE auth_schema.user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'EDITOR', 'VIEWER')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, company_id)
);

CREATE TABLE auth_schema.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON auth_schema.users(email);
CREATE INDEX idx_user_roles_user_id ON auth_schema.user_roles(user_id);
CREATE INDEX idx_user_roles_company_id ON auth_schema.user_roles(company_id);
CREATE INDEX idx_refresh_tokens_token ON auth_schema.refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON auth_schema.refresh_tokens(user_id);
