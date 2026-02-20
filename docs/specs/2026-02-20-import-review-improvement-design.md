# Melhoria na Tela de Revisão de Importação

**Data**: 2026-02-20
**Status**: Aprovado

## Problema

1. Transações importadas podem ter o tipo (A Pagar / A Receber) classificado incorretamente pelos parsers, dependendo do formato do extrato bancário (especialmente PDF). A coluna "Tipo" na tela de revisão mostra um badge estático — o usuário não percebe que pode/deve verificar.
2. Não existe paginação na tela de revisão. Extratos com centenas de transações renderizam todas de uma vez.
3. Não há mecanismo para forçar o usuário a revisar os itens antes de confirmar a importação.

## Solução

Três mudanças na tela `bank-import-review`, todas frontend-only (backend já suporta):

### 1. Coluna "Tipo" editável (dropdown)

- Substituir o badge estático por um `<select>` com opções `A Pagar` / `A Receber`.
- Estilo visual idêntico aos selects de Fornecedor e Categoria.
- Ao alterar, chamar `updateItemField(item, 'accountType', value)` — endpoint existente.
- Adicionar "Tipo..." na barra de ações em lote (bulk action bar) para alteração em massa.

### 2. Paginação client-side (25 itens/página)

- Todos os itens já vêm do backend de uma vez no `BankImport.items[]`.
- Paginação é puramente visual, sem mudanças no backend.
- Signals: `currentReviewPage`, computed `pagedItems()`, `totalReviewPages`.
- Componente de paginação no rodapé da tabela (mesmo estilo da tela de contas).
- Contadores `readyCount` e `totalCount` continuam computando sobre TODOS os itens.

### 3. Checkbox "Revisei todos os itens"

- Checkbox abaixo da tabela: "Revisei todos os itens e confirmo que os tipos estão corretos".
- Signal `reviewed = signal(false)`.
- Condição do botão "Confirmar Importação": `allReady() && reviewed() && !confirming()`.
- Tooltip atualizado quando desabilitado para incluir mensagem sobre revisão.

## Arquivos afetados

- `bank-import-review.component.html` — template (dropdown, paginação, checkbox)
- `bank-import-review.component.ts` — signals de paginação e reviewed
- Nenhum arquivo backend modificado
- Nenhuma mudança nos parsers (OFX/CSV/PDF)

## Fora do escopo

- Mudanças na lógica dos parsers
- Paginação server-side
- Edição de outros campos dos itens (data, valor, descrição)
