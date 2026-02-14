# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**FinDash** — A financial dashboard for Brazilian SMEs (PMEs) to manage accounts payable/receivable, import bank statements (OFX/CSV) with supplier linking, and visualize cash flow. The project is in early development (scaffolded, minimal application code).

The language used throughout the documentation and product is **Portuguese (Brazilian)**. User-facing strings, business terms, and product decisions are in pt-BR.

## Repository Structure

This is a monorepo with two separate projects (no monorepo tooling like Nx/Turborepo):

```
gestao-empresaial-frontend/   ← Angular 21 SPA (note: folder name has a typo)
gestao-empresarial-backend/   ← Spring Boot 4 API (Maven)
docs/                         ← Product docs (PRD, MVP scope, design guidelines, brief)
```

## Build & Run Commands

### Frontend (`gestao-empresaial-frontend/`)

```bash
npm install              # Install dependencies (npm 11.5.2)
npm start                # Dev server at http://localhost:4200
npm run build            # Production build
npm run watch            # Dev build with watch mode
npm test                 # Run tests (Vitest)
```

Component prefix: `app`. Angular CLI: `ng generate component features/my-feature/components/my-component`.

### Backend (`gestao-empresarial-backend/`)

```bash
./mvnw spring-boot:run   # Run the application
./mvnw clean install     # Build
./mvnw test              # Run tests
```

Package: `gestao.com.example.gestaoempresarialbackend`. Spring Boot 4.0.2, Java 17, Maven wrapper included.

## Architecture

### High-Level

```
[Browser] → [Angular SPA] → [API Gateway (Spring Cloud Gateway)]
                                    ↓
                  ┌──────────────────────────────────┐
                  │  Auth Service (JWT, RBAC)         │
                  │  Financial Service (accounts,     │
                  │    imports, categories,            │
                  │    suppliers, clients)             │
                  │  Company Service (multi-tenant)    │
                  └──────────────────────────────────┘
                                    ↓
                            [PostgreSQL]
```

Microservices architecture planned. Currently a single Spring Boot app with Spring Data JPA + Spring MVC. No database configured yet (only `spring-boot-starter-data-jpa` dependency).

### Frontend Architecture (Planned)

```
src/app/
├── core/          # Singleton services (auth guards, interceptors, layout, config)
├── features/      # Lazy-loaded feature modules (dashboard, accounts-payable,
│                  #   accounts-receivable, bank-import, categories, suppliers,
│                  #   clients, company-settings, user-management)
└── shared/        # Reusable components, pipes, directives (ui/, financial/, company/)
```

Each feature follows: `components/`, `services/`, `models/`.

### Backend Architecture (Planned)

Hexagonal/Ports & Adapters with DDD. Three microservices:
- **Auth Service** — Users, JWT auth, roles/permissions (admin/editor/viewer)
- **Financial Service** — Accounts payable/receivable, bank imports, categories, suppliers, clients
- **Company Service** — Multi-tenant company management, data segregated by `company_id`

## Tech Stack & Key Conventions

### Frontend
- **Angular 21** with standalone components (do NOT set `standalone: true` — it's the default since v20)
- **Tailwind CSS 4** via PostCSS — global import in `src/styles.css`
- **PrimeNG** planned for complex components (tables, calendars, file upload, charts) — not yet installed
- **Vitest 4** for testing (global functions, no imports needed). Test files: `*.spec.ts`
- **TypeScript 5.9** with strict mode and all Angular strict compiler options enabled
- **Signals** for local state, `computed()` for derived state — no NgRx
- **Reactive Forms** only (not template-driven)
- **Prettier** configured inline in package.json (100 char width, single quotes, Angular HTML parser)

### Angular Patterns (from PRD)
- `input()` / `output()` functions, not `@Input()` / `@Output()` decorators
- `inject()` function, not constructor injection
- `ChangeDetectionStrategy.OnPush` on all components
- Native control flow: `@if`, `@for`, `@switch` (not `*ngIf`, `*ngFor`)
- `class`/`style` bindings, not `ngClass`/`ngStyle`
- `readonly` for Angular-initialized properties, `protected` for template-only members
- Host bindings in `host` object (not `@HostBinding`/`@HostListener`)
- Lazy loading for feature routes
- Event handlers named by action: `saveAccount()` not `handleClick()`

### Backend
- **Spring Boot 4.0.2**, Java 17, Maven
- Spring Data JPA, Spring MVC (`spring-boot-starter-webmvc`)
- Spring Security with JWT + refresh token (planned)
- REST API only (no GraphQL, no WebSocket, no message queues)
- Synchronous communication between services

## Design System

Documented in `docs/DESIGN-GUIDELINES.md`:
- **Light mode only**, desktop-first
- Visual references: Linear, Resend, Vercel, Mercury, Brex
- Primary color: `#2563EB` (blue-600)
- Semantic: emerald-600 for revenue/positive, red-600 for expenses/negative, amber-600 for pending/warnings
- Typography: Inter (body), JetBrains Mono (financial values)
- Financial format: `R$ 1.234,56` (Brazilian), positive=emerald, negative=red, monospace font
- Cards: `rounded-lg`, prefer borders over shadows. Shadows for floating elements (modals, dropdowns)
- Status tags: Pendente (amber), Pago/Recebido (emerald), Vencido (red), Parcial (blue)
- Sidebar: 256px fixed, content max-width: 1280px
- Animations: 150-200ms, ease-out, subtle only
- Icons: Lucide Icons or PrimeIcons, 20px inline / 24px in cards

## Git Workflow

- **Toda feature nova ou plano de implementacao deve ser desenvolvido em uma branch separada** criada a partir da `master`.
- Naming convention: `feature/<nome-curto>` (ex: `feature/phase1-infra-auth`, `feature/phase2-company-service`)
- Fazer commits frequentes na branch durante o desenvolvimento.
- **Ao finalizar, criar um Pull Request para `master`** para revisao antes do merge. Nunca fazer merge direto na master.
- Nao fazer push para `master` sem PR aprovado.

## Constraints

- Desktop-first (mobile is future)
- No dark mode in MVP
- No i18n
- REST only, no GraphQL
- No micro-frontends
- Single PostgreSQL database (schema per service or logical segregation)
- Budget limits: initial bundle max 1MB error / 500kB warning; component styles max 8kB error / 4kB warning
- Browser support: Chrome 111+, Firefox 128+, Safari 16.4+
- WCAG AA accessibility required
- OFX/CSV import: max 5MB files (~10,000 transactions)

## Key Documentation

- `docs/PRD.md` — Full product requirements, user stories, technical architecture, edge cases
- `docs/MVP-SCOPE.md` — What's in/out of MVP, P0/P1/P2 features, definition of done
- `docs/DESIGN-GUIDELINES.md` — Complete design system (colors, typography, spacing, components)
- `docs/BRIEF.md` — Product brief, problem statement, value proposition
