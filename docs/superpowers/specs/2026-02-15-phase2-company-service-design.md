# Phase 2: Company Service + Multi-empresa — Design Spec

**Data:** 15/02/2026
**Status:** Approved

---

## Resumo

Phase 2 adiciona gestao de empresas e multi-tenancy ao FinDash. Usuarios criam empresas, convidam membros com roles (EDITOR/VIEWER), e alternam entre empresas via dropdown no header. O backend usa `company_schema` separado e o frontend forca criacao de empresa apos primeiro login.

---

## Decisoes de Design

| Decisao | Escolha |
|---------|---------|
| Schema do banco | `company_schema` separado (sem FK cross-schema, referencia por UUID) |
| Organizacao do codigo | Flat layered — classes adicionadas nos pacotes existentes (controller/, service/, entity/, etc.) |
| Onboarding | Forcado — apos primeiro login sem empresa, redireciona para `/empresas/nova` |
| Convites | Simples — se email ja registrado, ativa imediatamente. Se nao, armazena como INVITED e resolve no registro futuro |
| Seletor de empresa | Dropdown simples no header com nomes. Ultima empresa selecionada salva em localStorage |
| Campos editaveis | Nome (obrigatorio), CNPJ (opcional, validado), Segmento (opcional, texto livre) |

---

## Modelo de Dados

### company_schema.companies

| Coluna | Tipo | Notas |
|--------|------|-------|
| id | UUID (PK) | Auto-generated |
| name | VARCHAR(255) | NOT NULL, 2-255 chars |
| cnpj | VARCHAR(14) | UNIQUE, nullable, armazenado como 14 digitos raw |
| segment | VARCHAR(100) | nullable, texto livre |
| owner_id | UUID | NOT NULL, referencia auth_schema.users por valor |
| active | BOOLEAN | default TRUE |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

### company_schema.company_members

| Coluna | Tipo | Notas |
|--------|------|-------|
| id | UUID (PK) | Auto-generated |
| company_id | UUID (FK) | references companies ON DELETE CASCADE |
| user_id | UUID | nullable, preenchido quando usuario existe ou se registra. Referencia auth_schema.users por valor |
| invited_email | VARCHAR(255) | NOT NULL, email do convite. Usado para resolver convites pendentes no registro |
| status | VARCHAR(20) | ACTIVE, INVITED, REMOVED |
| invited_at | TIMESTAMP | NOT NULL |
| joined_at | TIMESTAMP | nullable, preenchido quando status muda para ACTIVE |

**Indices:**
- `company_members(company_id)`
- `company_members(user_id)`
- `company_members(invited_email)` — para busca no registro
- UNIQUE constraint: `(company_id, invited_email)` — impede convite duplicado por email

**Relacoes entre schemas:**
- `companies.owner_id` referencia `auth_schema.users.id` por valor (sem FK real)
- `company_members.user_id` referencia `auth_schema.users.id` por valor (sem FK real)
- Ao criar empresa: cria `CompanyMember` (status=ACTIVE) + `auth_schema.user_roles` (role=ADMIN)

**Onde fica o role:**
- A role do membro (ADMIN/EDITOR/VIEWER) e armazenada em `auth_schema.user_roles`, nao em `company_members`. O CompanyService le/escreve `user_roles` para operacoes de role. `company_members` controla apenas membership e status.

**Nota sobre role-change endpoint:**
- O MVP spec original colocava alteracao de role no Auth Service (`PUT /api/auth/users/{id}/role`). Como a arquitetura foi refatorada para single-module, movemos para o Company domain (`PUT /api/companies/{id}/members/{userId}/role`) pois roles sao per-company.

---

## API Endpoints

Todos protegidos por JWT. `X-Company-Id` header necessario para operacoes de membros.

### Company CRUD

| Metodo | Path | Auth | Descricao |
|--------|------|------|-----------|
| POST | `/api/companies` | Autenticado | Cria empresa. Criador vira owner + ADMIN |
| GET | `/api/companies` | Autenticado | Lista empresas do usuario |
| GET | `/api/companies/{id}` | Membro | Detalhes da empresa |
| PUT | `/api/companies/{id}` | ADMIN | Atualiza nome, CNPJ, segmento |

### Gestao de Membros

| Metodo | Path | Auth | Descricao |
|--------|------|------|-----------|
| GET | `/api/companies/{id}/members` | ADMIN/EDITOR | Lista membros com roles e status |
| POST | `/api/companies/{id}/members/invite` | ADMIN | Convite por email + role |
| PUT | `/api/companies/{id}/members/{userId}/role` | ADMIN | Altera role do membro |
| DELETE | `/api/companies/{id}/members/{userId}` | ADMIN | Remove membro (status=REMOVED) |

### Validacoes

- CNPJ: opcional, mas se fornecido deve ter formato valido (14 digitos, digitos verificadores). Unique.
- Nome: obrigatorio, 2-255 caracteres.
- Convite duplicado (mesmo email, mesma empresa, status ACTIVE ou INVITED): 409 Conflict.
- Owner nao pode ser removido.
- Owner nao pode ter role alterado (sempre ADMIN).
- Nao pode convidar como ADMIN (apenas EDITOR ou VIEWER).
- Nao pode alterar role do ultimo ADMIN ativo da empresa (deve sempre existir pelo menos 1 ADMIN).
- Nao pode remover o ultimo ADMIN ativo da empresa.

---

## Backend — Estrutura de Arquivos

### Novos arquivos (15)

**Entity:**
- `entity/Company.java` — @Entity, schema `company_schema`, tabela `companies`
- `entity/CompanyMember.java` — @Entity, schema `company_schema`, tabela `company_members`
- `entity/MemberStatus.java` — Enum: ACTIVE, INVITED, REMOVED

**Repository:**
- `repository/CompanyRepository.java` — findByOwnerId, queries por membership
- `repository/CompanyMemberRepository.java` — findByCompanyId, findByUserIdAndStatus, findByInvitedEmailAndStatus

**DTO:**
- `dto/CreateCompanyRequestDTO.java` — name, cnpj (optional), segment (optional)
- `dto/UpdateCompanyRequestDTO.java` — name, cnpj (optional), segment (optional)
- `dto/CompanyResponseDTO.java` — id, name, cnpj, segment, ownerName, role, active
- `dto/InviteMemberRequestDTO.java` — email, role
- `dto/CompanyMemberResponseDTO.java` — userId, userName, email, role, status, joinedAt

**Mapper:**
- `mapper/CompanyMapper.java` — MapStruct

**Service:**
- `service/CompanyService.java` — Interface
- `service/impl/CompanyServiceImpl.java` — Implementacao

**Controller:**
- `controller/CompanyController.java` — REST endpoints

**Migration:**
- `resources/db/migration/V2__create_company_tables.sql`

### Arquivos modificados (2)

- `config/SecurityConfig.java` — Adicionar `/api/companies/**` nas rotas autenticadas
- `service/impl/AuthServiceImpl.java` — No `register()`, resolver convites pendentes (INVITED → ACTIVE)

---

## Frontend — Estrutura de Arquivos

### Novos arquivos (8)

```
features/company/
├── company.routes.ts
├── pages/
│   ├── create-company/
│   │   ├── create-company.component.ts
│   │   └── create-company.component.html
│   ├── company-settings/
│   │   ├── company-settings.component.ts
│   │   └── company-settings.component.html
│   └── user-management/
│       ├── user-management.component.ts
│       └── user-management.component.html
```

- `core/services/company.service.ts` — Signal com empresa ativa, CRUD, lista empresas, localStorage

### Arquivos modificados (5)

- `core/interceptors/auth.interceptor.ts` — Adicionar header `X-Company-Id`
- `core/guards/auth.guard.ts` — Verificar se usuario tem empresas, redirecionar para `/empresas/nova` se nao
- `layout/header/header.component.ts` + `.html` — Dropdown seletor de empresa
- `layout/sidebar/sidebar.component.ts` + `.html` — Link "Configuracoes"
- `app.routes.ts` — Adicionar rotas de company

### Rotas

| Path | Component | Descricao |
|------|-----------|-----------|
| `/empresas/nova` | CreateCompanyComponent | Criacao forcada apos primeiro login |
| `/configuracoes` | CompanySettingsComponent | Editar nome, CNPJ, segmento |
| `/configuracoes/usuarios` | UserManagementComponent | Listar, convidar, alterar roles, remover membros |

---

## Comportamentos e Edge Cases

### Fluxo pos-login

1. AuthService faz login com sucesso → CompanyService busca empresas do usuario
2. Se nao tem empresas → redireciona para `/empresas/nova`
3. Se tem empresas → seta ultima usada (localStorage) ou primeira como ativa → vai para `/dashboard`
4. Dropdown no header permite trocar a qualquer momento

### Fluxo de convite

1. ADMIN digita email + seleciona role (EDITOR ou VIEWER)
2. Backend verifica se email existe em `auth_schema.users`:
   - **Existe:** Cria `CompanyMember` status=ACTIVE + `UserRole` → membro ve empresa imediatamente
   - **Nao existe:** Cria `CompanyMember` status=INVITED → resolvido quando email se registrar
3. Convite duplicado (mesmo email + empresa, status ACTIVE ou INVITED) → 409 Conflict

### Hook no registro para convites pendentes

- `AuthServiceImpl.register()` modificado: apos criar usuario, busca `CompanyMember` com status=INVITED onde `user_id` ainda nao foi setado (busca por email via join ou campo email na tabela)
- Para cada convite pendente: seta `user_id`, status=ACTIVE, `joined_at`, cria `UserRole`

**Nota:** `company_members` precisa de um campo `invited_email` (VARCHAR 255) para armazenar o email do convite quando o usuario ainda nao existe. Quando o usuario se registra, o sistema busca por `invited_email` + status=INVITED.

### Validacao de CNPJ

- Formato aceito na entrada: `XX.XXX.XXX/XXXX-XX` ou 14 digitos raw
- Armazenamento no banco: 14 digitos raw (sem pontuacao). Formatacao feita no frontend/DTO de resposta
- Validacao dos digitos verificadores usando algoritmo padrao brasileiro
- Unique constraint — se outra empresa ja tem o mesmo CNPJ → 409 Conflict

### Protecao do owner

- Owner nao pode ser removido da empresa
- Role do owner nao pode ser alterado (sempre ADMIN)
- Tentativa retorna 403 Forbidden com mensagem explicativa

### Troca de empresa

- CompanyService atualiza signal da empresa ativa
- Interceptor pega novo `X-Company-Id` automaticamente
- Pagina atual recarrega dados (componentes usam `effect()` ou re-fetch ao detectar mudanca)

### Usuario removido enquanto logado

- Se um usuario removido faz request com `X-Company-Id` de empresa da qual foi removido, backend retorna 403
- Frontend intercepta 403 em requests com `X-Company-Id` → recarrega lista de empresas → redireciona para proxima empresa disponivel ou para `/empresas/nova` se nao tem nenhuma

### Desativacao de empresa

- Fora de escopo do Phase 2. O campo `active` existe na tabela mas nao ha endpoint para desativar. Sera implementado futuramente.

---

## Estrategia de Testes

### Backend

| Camada | Ferramenta | Escopo |
|--------|-----------|--------|
| CompanyServiceImpl | JUnit 5 + Mockito | Logica de criacao, convite, validacoes, protecao owner |
| CompanyController | @WebMvcTest | Serializacao, validacao, status codes, autorizacao |
| CompanyRepository | @DataJpaTest | Queries customizadas |

### Frontend

| Componente | Ferramenta | Escopo |
|------------|-----------|--------|
| CompanyService | Vitest | Signals, localStorage, HTTP mockado |
| CreateCompanyComponent | Vitest | Formulario, validacao, submit |
| UserManagementComponent | Vitest | Lista membros, convite, acoes |
