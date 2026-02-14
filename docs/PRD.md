# FinDash — Product Requirements Document

**Autor:** —  
**Data:** 10/02/2026  
**Status:** Draft

---

## Overview

FinDash é um dashboard financeiro empresarial simplificado para PMEs. Centraliza contas a pagar, contas a receber, fluxo de caixa e importação bancária com vínculo a fornecedores em uma interface limpa e moderna. Nasce da frustração real com ferramentas como Nibo, que oferecem filtros ruins, gráficos limitados e não permitem vincular lançamentos importados a fornecedores específicos.

---

## Problem

### O que está acontecendo?

Analistas financeiros e contadores de PMEs dependem de ferramentas como Nibo, Conta Azul e Omie para gestão financeira. Essas ferramentas apresentam problemas concretos: filtros de período de entrada e saída são ruins e limitados, não oferecem gráficos detalhados (como receita x despesa geral), o DRE é rígido e não permite customização, e não é possível importar lançamentos bancários e vinculá-los automaticamente a fornecedores específicos. Isso força analistas a manter planilhas paralelas, duplicar trabalho e aumentar risco de erros.

### Quem é afetado?

Duas personas principais são impactadas: analistas financeiros que operam o dia a dia financeiro de PMEs e contadores que gerenciam o financeiro de múltiplas empresas clientes.

### Qual o custo de não resolver?

Horas semanais gastas em workarounds manuais (planilhas paralelas, exportações e re-importações). Risco de erros em lançamentos e relatórios. Frustração e baixa produtividade. Dificuldade em ter visão clara e atualizada do financeiro da empresa.

### Como resolvem hoje?

Usam uma combinação de ferramentas como Nibo, Conta Azul, Omie — complementadas por planilhas Excel/Google Sheets para cobrir as lacunas. Alguns usam ERPs mais pesados, mas a complexidade não se justifica para PMEs.

---

## Goals

- [ ] **Goal 1:** Usuários conseguem lançar e visualizar contas a pagar/receber com filtros de período superiores ao Nibo → Métrica: NPS de filtros > 7/10 em feedback qualitativo
- [ ] **Goal 2:** Importação OFX/CSV com vínculo a fornecedores funciona de ponta a ponta → Métrica: > 60% dos lançamentos importados vinculados com sucesso
- [ ] **Goal 3:** Dashboard com gráficos é usado diariamente → Métrica: > 50% dos usuários acessam dashboard 3+ vezes por semana

---

## Non-Goals

- ❌ Emissão de nota fiscal (escopo futuro)
- ❌ Integração Open Finance / APIs bancárias diretas
- ❌ DRE customizado (será P1, versão 1.1)
- ❌ Conciliação bancária automatizada (será P1)
- ❌ App mobile
- ❌ Sistema de billing/assinatura
- ❌ API pública
- ❌ Dashboard avançado para contadores (visão multi-cliente)
- ❌ Internacionalização (i18n)
- ❌ Relatórios exportáveis PDF/Excel (será P1)

---

## User Stories

### Persona 1: Lucas — Analista Financeiro

> Lucas, 28 anos, analista financeiro em uma PME com 30 funcionários. Responsável por todo o controle financeiro: contas a pagar, receber, fluxo de caixa e relatórios para a diretoria. Usa Nibo hoje e complementa com planilhas. Sua maior dor é a falta de filtros decentes e não conseguir vincular extratos importados aos fornecedores corretos.

**Gestão de Contas:**
- Como analista, eu quero cadastrar contas a pagar com data de vencimento, valor, fornecedor e categoria para ter controle completo das obrigações financeiras
- Como analista, eu quero cadastrar contas a receber com data prevista, valor, cliente e categoria para acompanhar as receitas esperadas
- Como analista, eu quero criar lançamentos recorrentes (ex: aluguel mensal) para não precisar cadastrar manualmente todo mês
- Como analista, eu quero marcar contas como pagas/recebidas com data efetiva para manter o fluxo de caixa atualizado
- Como analista, eu quero editar e excluir lançamentos para corrigir erros

**Importação Bancária:**
- Como analista, eu quero importar arquivos OFX do meu banco para cadastrar lançamentos em lote sem digitação manual
- Como analista, eu quero importar arquivos CSV do meu banco para ter flexibilidade de formato
- Como analista, eu quero vincular lançamentos importados a fornecedores específicos para saber exatamente para quem foi cada pagamento
- Como analista, eu quero que o sistema sugira o fornecedor com base em lançamentos anteriores similares para agilizar a categorização
- Como analista, eu quero categorizar lançamentos importados em grupos/categorias para alimentar relatórios

**Dashboard e Gráficos:**
- Como analista, eu quero ver um gráfico de fluxo de caixa (entradas vs saídas ao longo do tempo) para entender a saúde financeira
- Como analista, eu quero ver um gráfico de receita x despesa geral para ter visão macro do financeiro
- Como analista, eu quero ver a evolução mensal de receitas e despesas para identificar tendências
- Como analista, eu quero filtrar todos os dados por período customizado (data início e fim) com filtros rápidos (hoje, semana, mês, trimestre, ano, custom) para análises específicas
- Como analista, eu quero filtrar por categoria, fornecedor e status (pago/pendente) para drill-down nos dados

**Categorias:**
- Como analista, eu quero ter grupos de categorias pré-definidos (Custo Operacional, Receita Operacional, etc.) para começar a usar sem configuração
- Como analista, eu quero renomear grupos de categorias para adaptar à nomenclatura da minha empresa
- Como analista, eu quero criar, editar e excluir categorias dentro de cada grupo para refletir minha estrutura real

### Persona 2: Mariana — Contadora

> Mariana, 35 anos, contadora autônoma que gerencia o financeiro de 5 empresas clientes. Precisa alternar entre empresas rapidamente e ter visão consolidada. Sua maior dor é a lentidão para trocar de contexto entre clientes e a falta de relatórios claros.

**Multi-empresa:**
- Como contadora, eu quero ter acesso a múltiplas empresas na mesma conta para não precisar de múltiplos logins
- Como contadora, eu quero alternar entre empresas com um clique para agilizar meu trabalho
- Como contadora, eu quero ver um seletor de empresa sempre visível para saber em qual contexto estou

**Gestão de Acesso:**
- Como administradora, eu quero convidar outros usuários para a empresa para que minha equipe tenha acesso
- Como administradora, eu quero definir permissões por usuário (admin, editor, viewer) para controlar quem pode alterar dados
- Como administradora, eu quero remover acesso de usuários para manter a segurança

---

## Solution

### Visão Geral

FinDash é uma aplicação web composta por um frontend Angular e um backend Java/Spring Boot em arquitetura de microservices. O usuário se cadastra, cria ou é convidado para uma empresa, e acessa um dashboard financeiro com visão completa de contas a pagar, contas a receber e fluxo de caixa.

O diferencial principal está em três pilares: filtros de período robustos e intuitivos (o que concorrentes fazem mal), gráficos detalhados com visão de receita x despesa geral (o que falta nos concorrentes), e importação OFX/CSV com vínculo automático a fornecedores (funcionalidade inexistente no Nibo).

A interface segue o padrão visual de ferramentas como Linear e Resend: clean, light mode, moderna, sem ruído visual. O sistema de categorias vem pré-configurado com grupos padrão do mercado (Custo Operacional, Receita Operacional, Despesas Administrativas, etc.) mas é totalmente customizável.

### Features Principais

| Feature | Descrição | Prioridade |
|---------|-----------|------------|
| Auth + Gestão de Usuários | Cadastro, login (JWT), convite de usuários, permissões (admin/editor/viewer) | Must have (P0) |
| Multi-empresa | Cadastro de empresas, seletor de empresa, um usuário acessa múltiplas empresas | Must have (P0) |
| Contas a Pagar | CRUD completo, recorrência, status (pendente/pago), data vencimento/pagamento, fornecedor, categoria | Must have (P0) |
| Contas a Receber | CRUD completo, recorrência, status (pendente/recebido), data prevista/recebimento, cliente, categoria | Must have (P0) |
| Importação OFX/CSV | Upload de arquivo, parsing, listagem de lançamentos para revisão, vínculo a fornecedor, categorização | Must have (P0) |
| Categorias e Grupos | Grupos pré-definidos, renomear grupos, CRUD de categorias por grupo | Must have (P0) |
| Dashboard e Gráficos | Fluxo de caixa, receita x despesa geral, evolução mensal | Must have (P0) |
| Filtros de Período | Filtros rápidos (hoje, semana, mês, trimestre, ano) + período customizado, filtros por categoria/fornecedor/status | Must have (P0) |
| Cadastro de Fornecedores | CRUD de fornecedores (nome, CNPJ/CPF, dados de contato) | Must have (P0) |
| Cadastro de Clientes | CRUD de clientes (nome, CNPJ/CPF, dados de contato) | Must have (P0) |
| DRE Padrão e Customizado | Relatório DRE alimentado pelas categorias/grupos configurados | Should have (P1) |
| Conciliação Bancária | Match entre lançamentos importados e contas cadastradas | Should have (P1) |
| Relatórios Exportáveis | Exportação de relatórios em PDF e Excel | Should have (P1) |

### User Flows

**Fluxo 1: Primeiro Acesso**
```
1. Usuário acessa a aplicação
2. Cria conta (email + senha)
3. Sistema autentica via JWT
4. Usuário cria sua primeira empresa (nome, CNPJ, segmento)
5. Sistema cria categorias e grupos padrão automaticamente
6. Usuário é redirecionado ao dashboard (vazio, com onboarding básico)
```

**Fluxo 2: Lançamento Manual de Conta a Pagar**
```
1. Usuário clica em "Nova Conta a Pagar"
2. Preenche: descrição, valor, data vencimento, fornecedor, categoria, recorrência (opcional)
3. Salva o lançamento
4. Lançamento aparece na lista de contas a pagar e no dashboard
5. Quando pago, usuário marca como pago e informa data de pagamento
```

**Fluxo 3: Importação OFX/CSV com Vínculo a Fornecedor**
```
1. Usuário acessa "Importar Extrato"
2. Faz upload do arquivo OFX ou CSV
3. Sistema faz parsing e exibe lista de lançamentos encontrados
4. Para cada lançamento, sistema sugere fornecedor (baseado em histórico) e categoria
5. Usuário revisa, ajusta vínculos se necessário
6. Confirma importação
7. Lançamentos são criados com fornecedor e categoria vinculados
```

**Fluxo 4: Visualização do Dashboard**
```
1. Usuário acessa o dashboard
2. Vê gráficos: fluxo de caixa, receita x despesa, evolução mensal
3. Usa filtros rápidos (mês atual, trimestre, ano) ou define período customizado
4. Gráficos e totalizadores atualizam em tempo real
5. Pode filtrar por categoria ou fornecedor para drill-down
```

**Fluxo 5: Gestão de Acesso Multi-empresa**
```
1. Admin acessa "Configurações > Usuários"
2. Convida novo usuário por email
3. Define permissão (admin, editor, viewer)
4. Usuário convidado recebe email, cria conta (se não tiver) e acessa a empresa
5. Contadora com múltiplas empresas usa o seletor no header para alternar
```

---

## Technical Approach

### Stack

- **Frontend:** Angular (Latest) com Standalone Components, Signals, RxJS, Modular Architecture
- **Styling:** Tailwind CSS + PrimeNG
- **Backend:** Java 21+, Spring Boot (Spring MVC, Spring Data JPA, Spring Security com JWT, Spring Web/RestController)
- **Arquitetura Backend:** Microservices com Spring Cloud
- **Testes:** Vitest (frontend), JUnit (backend)
- **Conceitos:** Clean Code, SOLID, DDD, TDD, Hexagonal/Ports & Adapters

### Arquitetura de Alto Nível

```
[Browser] → [Angular SPA] → [API Gateway (Spring Cloud Gateway)]
                                      ↓
                    ┌─────────────────────────────────────┐
                    │          Microservices               │
                    │                                      │
                    │  ┌──────────┐  ┌──────────────────┐ │
                    │  │ Auth     │  │ Financial        │ │
                    │  │ Service  │  │ Service          │ │
                    │  │          │  │ (contas, import, │ │
                    │  │ (users,  │  │  categorias,     │ │
                    │  │  roles,  │  │  fornecedores,   │ │
                    │  │  JWT)    │  │  clientes)       │ │
                    │  └──────────┘  └──────────────────┘ │
                    │                                      │
                    │  ┌──────────────────┐               │
                    │  │ Company          │               │
                    │  │ Service          │               │
                    │  │ (empresas,       │               │
                    │  │  multi-tenant)   │               │
                    │  └──────────────────┘               │
                    └─────────────────────────────────────┘
                                      ↓
                              [PostgreSQL]
```

### Estrutura Frontend (Angular)

Seguindo as folder-rules do projeto:

```
src/
├── app/
│   ├── core/                          # Registrado em app.config.ts
│   │   ├── auth/
│   │   │   ├── guards/
│   │   │   ├── interceptors/
│   │   │   ├── services/
│   │   │   └── models/
│   │   ├── layout/
│   │   │   ├── components/            # header, sidebar, company-selector
│   │   │   └── services/
│   │   └── config/
│   │       └── services/              # app config, environment
│   │
│   ├── features/
│   │   ├── dashboard/
│   │   │   ├── components/            # chart-widgets, filters, summary-cards
│   │   │   ├── services/
│   │   │   └── models/
│   │   ├── accounts-payable/
│   │   │   ├── components/            # list, form, detail
│   │   │   ├── services/
│   │   │   └── models/
│   │   ├── accounts-receivable/
│   │   │   ├── components/
│   │   │   ├── services/
│   │   │   └── models/
│   │   ├── bank-import/
│   │   │   ├── components/            # upload, review-list, supplier-match
│   │   │   ├── services/
│   │   │   └── models/
│   │   ├── categories/
│   │   │   ├── components/
│   │   │   ├── services/
│   │   │   └── models/
│   │   ├── suppliers/
│   │   │   ├── components/
│   │   │   ├── services/
│   │   │   └── models/
│   │   ├── clients/
│   │   │   ├── components/
│   │   │   ├── services/
│   │   │   └── models/
│   │   ├── company-settings/
│   │   │   ├── components/
│   │   │   ├── services/
│   │   │   └── models/
│   │   └── user-management/
│   │       ├── components/
│   │       ├── services/
│   │       └── models/
│   │
│   └── shared/
│       ├── ui/
│       │   ├── components/            # buttons, modals, date-range-picker, data-table
│       │   └── directives/
│       ├── financial/
│       │   ├── models/                # Transaction, Account, Category, etc.
│       │   ├── pipes/                 # currency, date-format
│       │   └── services/              # financial calculations
│       └── company/
│           ├── models/
│           └── services/
```

### Diretrizes Técnicas Angular

Conforme angular-best-practices.md e angular-style-guide.md:

- Standalone Components (sem NgModules). NÃO setar `standalone: true` nos decorators (default no Angular v20+)
- Signals para state management local, `computed()` para estado derivado
- `input()` e `output()` functions ao invés de decorators
- `ChangeDetectionStrategy.OnPush` em todos os componentes
- Native control flow (`@if`, `@for`, `@switch`) nos templates
- `inject()` ao invés de constructor injection
- `class` e `style` bindings ao invés de `ngClass`/`ngStyle`
- Reactive Forms ao invés de Template-driven
- `readonly` em propriedades inicializadas pelo Angular
- `protected` em membros usados apenas pelo template
- Lazy loading para feature routes
- `NgOptimizedImage` para imagens estáticas
- Host bindings no objeto `host` do decorator (não usar `@HostBinding`/`@HostListener`)
- Nomes de event handlers pelo que fazem, não pelo evento (ex: `saveAccount()` não `handleClick()`)
- Testes unitários com Vitest (funções globais, sem import)

### Integrações

- [ ] Spring Security + JWT (auth)
- [ ] Spring Data JPA + PostgreSQL
- [ ] Spring Cloud Gateway (API Gateway)
- [ ] Parsing de OFX (biblioteca Java: ofx4j ou similar)
- [ ] Parsing de CSV (Apache Commons CSV ou OpenCSV)

### Constraints

- Desktop-first (mobile é futuro)
- Light mode only
- REST API (sem GraphQL)
- Sem real-time/WebSocket (polling é aceitável para o MVP)
- Sem message queues
- Sem micro-frontends
- Um banco PostgreSQL (schema por serviço ou shared com segregação lógica)

---

## Requisitos Não-Funcionais

| Requisito | Especificação |
|-----------|---------------|
| Performance | Dashboard carrega em < 2s com até 10.000 lançamentos |
| Segurança | JWT com refresh token, RBAC (admin/editor/viewer), dados isolados por empresa |
| Acessibilidade | WCAG AA, passar todos os checks AXE |
| Importação | Suporte a arquivos OFX e CSV de até 5MB (aprox. 10.000 lançamentos) |
| Browser Support | Chrome 111+, Firefox 128+, Safari 16.4+ |
| Responsividade | Desktop-first, responsivo mas não otimizado para mobile |

---

## Edge Cases e Regras de Negócio

### Importação OFX/CSV
- Arquivo com formato inválido: exibir mensagem de erro clara indicando o problema
- Lançamentos duplicados (mesmo valor, data e descrição): alertar o usuário antes de importar
- Arquivo vazio: exibir mensagem informativa
- Arquivo acima do limite (5MB): bloquear upload com mensagem
- Encoding incorreto no CSV: tentar detectar automaticamente (UTF-8, ISO-8859-1)

### Contas a Pagar/Receber
- Conta com data de vencimento no passado: permitir cadastro mas exibir indicador visual de atraso
- Lançamento recorrente: gerar próximos lançamentos automaticamente (até 12 meses à frente)
- Exclusão de lançamento recorrente: perguntar se exclui apenas este ou todos os futuros
- Valor zero ou negativo: bloquear com validação
- Conta já paga que é editada: manter status de pago e registrar alteração

### Multi-empresa e Permissões
- Usuário viewer tenta editar: interface não exibe botões de ação, API retorna 403
- Admin remove o último admin da empresa: bloquear ação
- Usuário é removido de uma empresa enquanto logado: redirecionar para seletor de empresas no próximo request
- Empresa sem lançamentos: dashboard exibe estado vazio com orientação de primeiro uso

### Categorias
- Grupo de categoria é excluído: bloquear se houver lançamentos vinculados, ou exigir reclassificação
- Categoria padrão é excluída: permitir (o usuário tem controle total)
- Duas categorias com mesmo nome no mesmo grupo: bloquear com validação

---

## Success Metrics

| Métrica | Baseline | Target | Como medir |
|---------|----------|--------|------------|
| Empresas cadastradas | 0 | 20 em 3 meses | Contagem no banco |
| Retention 7-day | 0% | > 50% | Usuários ativos na semana / total cadastrados |
| Lançamentos via importação | 0 | 100+ em 3 meses | Contagem de lançamentos com origem "importação" |
| Tempo para lançar conta a pagar | — | < 30 segundos | Medição de UX em sessões de teste |
| NPS de filtros | — | > 7/10 | Feedback qualitativo |

---

## Risks & Assumptions

### Assumptions
- Analistas financeiros de PMEs estão dispostos a testar uma ferramenta nova
- O formato OFX/CSV dos principais bancos brasileiros é suficientemente padronizado para parsing confiável
- 3 níveis de permissão (admin, editor, viewer) cobrem os casos de uso iniciais
- Categorias pré-definidas (Custo Operacional, Receita Operacional, etc.) atendem a maioria das PMEs

### Risks

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Parsing de OFX falha com bancos específicos | Alta | Alto | Testar com extratos de 5+ bancos antes do launch, ter fallback para CSV |
| Scope creep durante desenvolvimento | Alta | Alto | Revisar MVP-SCOPE.md semanalmente, dizer não a features P1+ |
| Concorrentes adicionam features similares | Média | Médio | Velocidade de execução e foco em UX superior |
| Usuários não migram de ferramentas existentes | Média | Alto | Permitir importação de dados de outras ferramentas, onboarding simples |
| Complexidade de microservices atrasa MVP | Média | Alto | Começar com 2-3 serviços bem definidos, não fragmentar demais |

---

## Open Questions

- [ ] Quais bancos brasileiros priorizar para teste de parsing OFX/CSV?
- [ ] O DRE padrão deve seguir qual modelo contábil? (CPC, simplificado, customizado?)
- [ ] Qual o limite de empresas por conta de usuário no plano free?
- [ ] Precisa de audit trail (log de alterações) no MVP ou pode ser futuro?
- [ ] Qual serviço de email para convites de usuários? (SendGrid, SES, etc.)
- [ ] Deploy: qual cloud? (AWS, GCP, Azure, ou plataforma simples como Railway/Render?)

---

## Appendix

### Concorrentes Analisados

| Concorrente | Pontos Fortes | Pontos Fracos |
|-------------|---------------|---------------|
| Nibo | Completo, mercado estabelecido | UX ruim, filtros limitados, sem vínculo importação-fornecedor |
| Conta Azul | Foco em pequenas empresas, boa adoção | Complexidade crescente, gráficos genéricos |
| Omie | ERP completo, integrações | Pesado demais para PMEs, curva de aprendizado alta |
| Planilhas | Flexível, familiar | Manual, sem automação, propenso a erros |
