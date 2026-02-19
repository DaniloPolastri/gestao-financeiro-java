# FinDash - MVP Scope

**Data:** 10/02/2026  
**Versão:** 1.0

---

## Visão do MVP

**Em uma frase, o que o MVP faz?**

> Permite que analistas financeiros de PMEs gerenciem contas a pagar/receber, importem extratos bancários (OFX/CSV) com vínculo a fornecedores e visualizem gráficos financeiros detalhados com filtros robustos.

**Qual hipótese estamos testando?**

> Analistas financeiros de PMEs estão frustrados o suficiente com ferramentas atuais (Nibo, Conta Azul) para migrar para uma ferramenta com melhor UX, filtros superiores e importação bancária com vínculo a fornecedores.

**Como saberemos que funcionou?**

> 20 empresas cadastradas em 3 meses, com 50%+ de retenção semanal e 100+ lançamentos via importação realizados.

---

## Escopo: O que ENTRA

### Must Have (P0) - Sem isso não lança

| Feature | Descrição | Critério de Done |
|---------|-----------|-----------------|
| Auth + JWT | Cadastro, login, logout, refresh token com Spring Security | Usuário consegue criar conta, logar e manter sessão |
| Gestão de Usuários | Convite por email, permissões (admin/editor/viewer), remoção | Admin consegue convidar, definir role e remover usuários |
| Multi-empresa | Criar empresa, seletor de empresa, um usuário acessa N empresas | Usuário alterna entre empresas sem re-login |
| Contas a Pagar | CRUD, recorrência, status (pendente/pago), vínculo a fornecedor e categoria | Analista cria, edita, paga e exclui contas a pagar |
| Contas a Receber | CRUD, recorrência, status (pendente/recebido), vínculo a cliente e categoria | Analista cria, edita, recebe e exclui contas a receber |
| Importação OFX/CSV | Upload, parsing, tela de revisão, vínculo a fornecedor, categorização, sugestão baseada em histórico | Analista importa arquivo e vincula lançamentos a fornecedores |
| Cadastro de Fornecedores | CRUD (nome, CNPJ/CPF, contato) | Fornecedores disponíveis para vínculo em contas e importações |
| Cadastro de Clientes | CRUD (nome, CNPJ/CPF, contato) | Clientes disponíveis para vínculo em contas a receber |
| Categorias e Grupos | Grupos pré-definidos, renomear, CRUD de categorias por grupo | Analista customiza estrutura de categorias |
| Dashboard | Gráficos: fluxo de caixa, receita x despesa geral, evolução mensal | Dashboard carrega com dados reais e gráficos interativos |
| Filtros | Período (hoje/semana/mês/trimestre/ano/custom), categoria, fornecedor, status | Filtros funcionam em todas as listagens e no dashboard |

### Should Have (P1) - Importante, mas pode esperar v1.1

| Feature | Descrição | Por que não é P0 |
|---------|-----------|-----------------|
| DRE Padrão e Customizado | Relatório DRE alimentado pelas categorias/grupos | Pode validar o core (lançamentos + dashboard) sem DRE |
| Conciliação Bancária | Match entre lançamentos importados e contas cadastradas | Importação com vínculo manual já resolve a dor principal |
| Relatórios PDF/Excel | Exportação de relatórios e listagens | Gráficos na tela são suficientes para validação |

### Could Have (P2) - Nice to have

| Feature | Descrição | Quando considerar |
|---------|-----------|-------------------|
| Notificações de vencimento | Alerta para contas a pagar próximas do vencimento | Quando tiver base de usuários ativa |
| Dashboard comparativo | Comparação período atual vs anterior | Após validar que dashboard básico é usado |
| Bulk actions | Marcar múltiplas contas como pagas, excluir em lote | Quando volume de lançamentos por empresa justificar |

---

## Escopo: O que NÃO ENTRA

### Explicitamente Fora do MVP

| Feature | Por que não entra | Quando reconsiderar |
|---------|-------------------|---------------------|
| Emissão de Nota Fiscal | Escopo enorme, requer integração com Sefaz, fora do core | v2.0+ após validação |
| Open Finance / API bancária | Complexidade técnica e regulatória alta | Quando MVP estiver validado e houver demanda |
| App Mobile | Desktop-first, público-alvo trabalha em desktop | Após validação com 100+ usuários |
| Sistema de Billing/Assinatura | MVP é free para validação | Quando decidir monetizar |
| API Pública | Sem demanda de integração no início | Quando parceiros/integradores pedirem |
| Dashboard do Contador (multi-cliente avançado) | Multi-empresa básica cobre o cenário inicial | Quando contadores representarem segmento relevante |
| Internacionalização (i18n) | Foco no mercado brasileiro | Se expandir para outros países |
| Dark mode | Não agrega valor para validação | Quando tiver base ativa pedindo |

### Tentações Comuns a Evitar

- [x] Dashboard de admin elaborado — admin simples é suficiente
- [x] Analytics avançados — gráficos básicos primeiro
- [x] Múltiplas integrações — OFX/CSV cobre o cenário
- [x] Multi-tenancy complexo — segregação por company_id no banco
- [x] Internacionalização (i18n)
- [x] Mobile app nativo
- [x] API pública
- [x] Marketplace/plugins
- [x] Billing complexo (múltiplos planos)

---

## Decisões de Simplificação

### Autenticação
- [x] Spring Boot Security com JWT + refresh token
- [ ] ~~Magic link only~~
- [ ] ~~Google OAuth only~~ (considerar para v1.1)

### Billing
- [x] Free only no MVP
- [ ] ~~Planos pagos~~
- [ ] ~~Stripe Checkout~~

### UI/UX
- [x] Light mode only
- [x] Desktop-first (mobile depois)
- [x] PrimeNG + Tailwind
- [x] Sem onboarding elaborado (estado vazio guia o usuário)

### Features
- [x] CRUD básico primeiro
- [x] Sem bulk actions
- [x] Sem export PDF/Excel (P1)
- [x] Sem histórico/versioning
- [x] Sem real-time (refresh manual ou polling)

---

## Personas no MVP

### Persona Principal (foco total)

**Nome:** Lucas — Analista Financeiro  
**Quem é:** Analista financeiro em PME (20-100 funcionários), responsável pelo controle financeiro diário  
**Job to be Done:** Controlar entradas e saídas financeiras da empresa com clareza e agilidade, sem depender de planilhas paralelas

### Persona Secundária (suportada mas não foco)

**Nome:** Mariana — Contadora  
**Quem é:** Contadora autônoma que gerencia múltiplas empresas  
**Job to be Done:** Alternar entre empresas clientes rapidamente e ter visão clara do financeiro de cada uma

### Personas FORA do MVP

| Persona | Por que não agora |
|---------|-------------------|
| Dono de MEI/Micro | Necessidades muito simples, não justifica a complexidade de permissões e categorias |
| Diretor Financeiro de empresa grande | Precisa de aprovações, audit trail, integrações ERP — escopo enterprise |

---

## Fluxos Críticos

### Fluxo 1: Importação OFX/CSV com Vínculo a Fornecedor

```
1. Analista acessa "Importar Extrato"
2. Seleciona arquivo OFX ou CSV do computador
3. Sistema valida formato e tamanho (< 5MB)
4. Sistema faz parsing e exibe lista de lançamentos em tabela de revisão
5. Para cada lançamento, sistema sugere fornecedor (baseado em descrição similar anterior)
6. Analista revisa e ajusta vínculos (fornecedor + categoria) onde necessário
7. Analista confirma importação
8. Sistema cria lançamentos como contas a pagar com fornecedor e categoria vinculados
9. Dashboard e listagens são atualizados
```

### Fluxo 2: Gestão de Contas a Pagar

```
1. Analista acessa "Contas a Pagar"
2. Vê listagem com filtros (período, status, fornecedor, categoria)
3. Clica em "Nova Conta"
4. Preenche formulário (descrição, valor, vencimento, fornecedor, categoria, recorrência)
5. Salva lançamento
6. Quando paga, clica em "Marcar como Pago" e informa data de pagamento
7. Lançamento muda de status e impacta fluxo de caixa
```

### Fluxo 3: Visualização do Dashboard

```
1. Analista acessa Dashboard (tela inicial após login)
2. Vê cards de resumo: total a pagar, total a receber, saldo
3. Vê gráfico de fluxo de caixa (entradas vs saídas no tempo)
4. Vê gráfico de receita x despesa geral
5. Vê gráfico de evolução mensal
6. Aplica filtro de período (ex: último trimestre)
7. Todos os gráficos e cards atualizam
```

---

## Stack do MVP

### Escolhas Definitivas

| Camada | Tecnologia | Justificativa |
|--------|------------|---------------|
| Frontend | Angular (Latest) + Standalone Components + Signals | Performance, DX, reatividade |
| Styling | Tailwind CSS + PrimeNG | Speed de desenvolvimento, componentes prontos |
| Backend | Java 21+ + Spring Boot (MVC + Data JPA + Security) | Robusto, ecossistema maduro |
| Arquitetura | Microservices com Spring Cloud Gateway | Escalabilidade, separação de responsabilidades |
| Auth | Spring Security + JWT | Standard, seguro |
| Database | PostgreSQL | Confiável, grátis, bom para financeiro |
| Testes Frontend | Vitest | Rápido, funções globais sem import |
| Testes Backend | JUnit 5 | Standard Java |
| Conceitos | Clean Code, SOLID, DDD, TDD, Hexagonal Architecture | Qualidade e manutenibilidade |

### O que NÃO usar (complexidade desnecessária)

- [x] GraphQL (REST é suficiente)
- [x] State management externo/NgRx (Signals é suficiente)
- [x] Micro-frontends
- [x] Kubernetes (deploy simples)
- [x] Multiple databases (um PostgreSQL com schema/segregação lógica)
- [x] Message queues (comunicação síncrona entre serviços)
- [x] WebSockets (polling é aceitável)

---

## Definition of Done (MVP)

O MVP está pronto quando:

- [ ] Todas as features P0 funcionando end-to-end
- [ ] Fluxos críticos testados (importação, contas a pagar, dashboard)
- [ ] Testes unitários escritos para services e componentes críticos
- [ ] Deploy em produção funcionando
- [ ] Auth com JWT funcionando (cadastro, login, refresh, permissões)
- [ ] Multi-empresa funcional (criar, alternar, convidar)
- [ ] Importação OFX/CSV testada com extratos de pelo menos 3 bancos diferentes
- [ ] Dashboard com 3 gráficos funcionais e filtros de período
- [ ] Pelo menos 1 usuário real usando
- [ ] Métricas de sucesso rastreáveis (contagem de cadastros, lançamentos, acessos)

---

## Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Scope creep | Alta | Alto | Revisar este doc semanalmente, dizer não a tudo que não é P0 |
| Parsing OFX varia entre bancos | Alta | Alto | Testar com 5+ bancos, ter fallback CSV, tratar erros gracefully |
| Tech debt acumula | Média | Médio | Aceitar para MVP mas documentar, refatorar em v1.1 |
| Microservices adicionam complexidade | Média | Alto | Começar com 2-3 serviços, não fragmentar demais |
| Usuários não migram de ferramentas atuais | Média | Alto | Importação fácil, onboarding simples, free no MVP |

---

## Hipóteses a Validar

| Hipótese | Como validar | Sucesso = |
|----------|-------------|-----------|
| PMEs sentem dor com ferramentas financeiras atuais | Signups orgânicos | > 20 empresas em 3 meses |
| Importação com vínculo a fornecedor é diferencial | % de lançamentos importados vs manuais | > 40% dos lançamentos via importação |
| Interface simplificada gera retenção | Retention 7-day | > 50% dos usuários ativos na semana |
| Filtros e gráficos resolvem a dor | Feedback qualitativo | NPS de filtros > 7/10 |

---

## Próximos Passos Pós-MVP

Depois de validar o MVP, considerar:

1. [ ] **v1.1:** DRE padrão e customizado (alimentado por categorias)
2. [ ] **v1.1:** Conciliação bancária (match importação x contas)
3. [ ] **v1.1:** Relatórios exportáveis (PDF/Excel)
4. [ ] **v1.2:** Google OAuth como método de login adicional
5. [ ] **v1.2:** Notificações de vencimento (email ou in-app)
6. [ ] **v2.0:** Planos de assinatura com billing (Stripe)
7. [ ] **v2.0:** Open Finance / integração bancária direta
8. [ ] **v3.0:** Emissão de nota fiscal

---
S
## Regra de Ouro

Quando em dúvida se algo entra no MVP, pergunte:

> "Posso validar que analistas financeiros querem uma ferramenta financeira mais simples com melhor importação bancária SEM essa feature?"

Se sim → Não entra no MVP.
