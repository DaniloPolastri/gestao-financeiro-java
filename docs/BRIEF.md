# FinDash

**Data:** 10/02/2026  
**Autor:** —  
**Status:** Validating

---

## 💡 Problema

**Em uma frase:**
> Ferramentas financeiras existentes (Nibo, Conta Azul, Omie) são complexas demais, têm filtros ruins, gráficos limitados e não permitem vincular lançamentos bancários importados a fornecedores específicos.

**Contexto:**
Analistas financeiros e contadores de PMEs passam horas em ferramentas que dificultam tarefas básicas como filtrar por período, visualizar receita vs despesa e importar extratos com vínculo a fornecedores. A falta de um DRE customizável e gráficos detalhados obriga muitos a recorrer a planilhas paralelas, duplicando trabalho e aumentando risco de erro.

---

## ✅ Solução

**Em uma frase:**
> Um dashboard financeiro empresarial simplificado que centraliza contas a pagar/receber, fluxo de caixa e importação bancária com vínculo automático a fornecedores — tudo com uma interface limpa e gráficos detalhados.

**Como funciona:**
O usuário cadastra sua empresa, configura categorias e grupos (com padrões pré-definidos), e passa a lançar contas a pagar/receber manualmente ou via importação de arquivos OFX/CSV. O dashboard apresenta gráficos de fluxo de caixa, receita vs despesa e evolução mensal com filtros de período robustos. Lançamentos importados podem ser vinculados a fornecedores específicos automaticamente.

---

## 👤 Público-Alvo

**Persona principal:**
> Analista financeiro de PME que gerencia o financeiro de uma ou mais empresas e precisa de uma ferramenta mais simples e visual que os ERPs tradicionais.

**Early adopters:**
> Analistas financeiros e contadores que já usam Nibo, Conta Azul ou planilhas e estão frustrados com a falta de filtros, gráficos e a complexidade dessas ferramentas.

---

## 🎯 Proposta de Valor

**Por que escolher você?**
> Interface limpa com filtros robustos, gráficos detalhados e importação bancária com vínculo a fornecedores — o que as ferramentas atuais não fazem bem.

**Alternativas atuais:**
- Nibo: completo mas com UX ruim, filtros limitados, sem vínculo de importação a fornecedores
- Conta Azul / Omie: foco em micro/pequenas, complexidade crescente, gráficos genéricos
- Planilhas (Excel/Sheets): flexível mas manual, sem automação, propenso a erros

**Seu diferencial:**
- Importação OFX/CSV com vínculo automático a fornecedores (dor real não atendida)
- Filtros de período superiores aos concorrentes
- Gráficos detalhados (receita x despesa geral, fluxo de caixa, evolução mensal)
- Interface clean e moderna (referência: Linear, Resend)
- Categorias e grupos customizáveis com impacto direto no DRE

---

## 💰 Modelo de Negócio

**Monetização:**
> SaaS por assinatura mensal com 3 tiers (a definir). Pricing será feature futura — MVP lança free para validação.

**Pricing inicial:**
| Plano | Preço | Target |
|-------|-------|--------|
| Free (MVP) | R$ 0 | Validação com early adopters |
| Starter | A definir | PMEs pequenas |
| Pro | A definir | PMEs médias / Contadores |

---

## 📊 Métricas de Sucesso

**North Star Metric:**
> Número de lançamentos financeiros processados por semana (indica uso real e recorrente)

**Metas iniciais (3 meses):**
- [ ] 20 empresas cadastradas
- [ ] 50% dos usuários ativos na semana (retention 7-day)
- [ ] 100+ lançamentos via importação OFX/CSV realizados

---

## 🚀 MVP Scope

**O que entra:**
- Cadastro de empresas e multi-empresa por usuário
- Gestão de usuários com permissões (admin, editor, viewer)
- Contas a pagar e contas a receber (CRUD + recorrência)
- Importação de extratos OFX/CSV com vínculo a fornecedores
- Categorias e grupos customizáveis (com padrões pré-definidos)
- Dashboard com gráficos: fluxo de caixa, receita x despesa, evolução mensal
- Filtros de período robustos

**O que NÃO entra:**
- DRE (v1.1)
- Conciliação bancária automatizada (v1.1)
- Emissão de nota fiscal
- Open Finance / integração bancária direta
- Planos de assinatura e billing
- App mobile

---

## 🛠 Stack

| Camada | Tecnologia |
|--------|------------|
| Frontend | Angular (Latest), Standalone Components, Signals, RxJS, Modular Architecture + Tailwind + PrimeNG |
| Backend | Java 21+, Spring Boot (Spring MVC, Spring Data JPA, Spring Security com JWT), Spring Cloud |
| Arquitetura | Microservices (front e back em repositórios/pastas separados) |
| Conceitos | Clean Code, SOLID, DDD, TDD, Hexagonal/Ports & Adapters |
| Deploy | Não definido |

---

## ⏱ Timeline

| Marco | Data/Prazo |
|-------|------------|
| MVP pronto | Sem prazo definido |
| Primeiros usuários | Após MVP |
| Primeira receita | Após validação do MVP |

---

## ❓ Hipóteses a Validar

1. [ ] Analistas financeiros de PMEs sentem dor suficiente com ferramentas atuais para migrar para uma nova
2. [ ] Importação OFX/CSV com vínculo automático a fornecedores é um diferencial que atrai e retém
3. [ ] Uma interface mais limpa, com melhores filtros e gráficos, gera uso recorrente

---

## 🔗 Links

- Repo: —
- Docs: —
- Design: —
- Produção: —

---

## 📝 Notas

- Referência principal de concorrente a superar: Nibo
- Estilo visual: clean, moderno, light mode (referências: Linear, Resend, Vercel)
- Foco total em desktop-first; mobile é futuro
- Testes unitários com Vitest
