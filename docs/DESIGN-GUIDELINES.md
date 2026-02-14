# FinDash - Design Guidelines

**Data:** 10/02/2026  
**Estilo:** Clean, moderno, light mode  
**Referências:** Linear, Resend, Vercel

---

## Paleta de Cores

### Core

| Token | Hex | Uso |
|-------|-----|-----|
| **Primary** | `#2563EB` (blue-600) | CTAs, links, elementos interativos, ícones ativos |
| **Primary Hover** | `#1D4ED8` (blue-700) | Hover em botões e links |
| **Primary Light** | `#EFF6FF` (blue-50) | Backgrounds sutis, badges, selected states |
| **Primary Muted** | `#BFDBFE` (blue-200) | Borders de elementos ativos, progress indicators |

### Neutrals

| Token | Hex | Uso |
|-------|-----|-----|
| **Background** | `#FFFFFF` | Background principal |
| **Surface** | `#F9FAFB` (gray-50) | Cards, seções alternadas, sidebars |
| **Border** | `#E5E7EB` (gray-200) | Borders de cards, inputs, separadores |
| **Border Subtle** | `#F3F4F6` (gray-100) | Borders muito sutis, hover backgrounds |
| **Text Primary** | `#111827` (gray-900) | Títulos, texto principal |
| **Text Secondary** | `#6B7280` (gray-500) | Subtítulos, descrições, labels |
| **Text Muted** | `#9CA3AF` (gray-400) | Placeholders, texto desabilitado |

### Semantic

| Token | Hex | Uso |
|-------|-----|-----|
| **Success** | `#059669` (emerald-600) | Receitas, contas recebidas, valores positivos |
| **Success Light** | `#ECFDF5` (emerald-50) | Background de indicadores positivos |
| **Danger** | `#DC2626` (red-600) | Despesas, contas vencidas, erros, valores negativos |
| **Danger Light** | `#FEF2F2` (red-50) | Background de indicadores negativos |
| **Warning** | `#D97706` (amber-600) | Contas próximas do vencimento, alertas |
| **Warning Light** | `#FFFBEB` (amber-50) | Background de alertas |
| **Info** | `#2563EB` (blue-600) | Informações, tooltips |

### Gráficos

| Token | Hex | Uso |
|-------|-----|-----|
| **Chart Receita** | `#059669` (emerald-600) | Barras/linhas de receita |
| **Chart Despesa** | `#DC2626` (red-600) | Barras/linhas de despesa |
| **Chart Primary** | `#2563EB` (blue-600) | Linha principal, fluxo de caixa |
| **Chart Secondary** | `#8B5CF6` (violet-500) | Dados comparativos |
| **Chart Tertiary** | `#F59E0B` (amber-500) | Terceira série de dados |
| **Chart Grid** | `#F3F4F6` (gray-100) | Grid lines dos gráficos |

---

## Tipografia

### Font Family

**Principal:** `Inter` (Google Fonts)  
**Fallback:** `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif`

**Monospace (para valores financeiros):** `'JetBrains Mono'` ou `'Fira Code'` (Google Fonts)  
**Fallback mono:** `ui-monospace, 'Courier New', monospace`

### Escala Tipográfica

| Token | Size | Weight | Line Height | Uso |
|-------|------|--------|-------------|-----|
| **Display** | 36px (text-4xl) | 700 (bold) | 1.1 | Hero headline da landing page |
| **H1** | 30px (text-3xl) | 700 (bold) | 1.2 | Títulos de página |
| **H2** | 24px (text-2xl) | 600 (semibold) | 1.3 | Títulos de seção |
| **H3** | 20px (text-xl) | 600 (semibold) | 1.4 | Subtítulos, títulos de card |
| **H4** | 16px (text-base) | 600 (semibold) | 1.5 | Labels de grupo, cabeçalhos de tabela |
| **Body** | 14px (text-sm) | 400 (normal) | 1.6 | Texto principal do app |
| **Body Large** | 16px (text-base) | 400 (normal) | 1.6 | Texto da landing page |
| **Caption** | 12px (text-xs) | 400 (normal) | 1.5 | Labels, timestamps, metadata |
| **Financial** | 14px (text-sm) | 500 (medium) | 1.4 | Valores monetários (usar font mono) |
| **Financial Large** | 24px (text-2xl) | 700 (bold) | 1.2 | Totalizadores no dashboard (usar font mono) |

---

## Espaçamento

### Escala Base: 4px

| Token | Valor | Tailwind | Uso |
|-------|-------|----------|-----|
| **xs** | 4px | `p-1`, `gap-1` | Espaço mínimo entre ícone e texto |
| **sm** | 8px | `p-2`, `gap-2` | Padding interno de badges, espaço entre elementos inline |
| **md** | 12px | `p-3`, `gap-3` | Padding interno de inputs e botões small |
| **base** | 16px | `p-4`, `gap-4` | Padding padrão de cards, gap entre elementos |
| **lg** | 24px | `p-6`, `gap-6` | Padding de seções dentro de cards, gap entre cards |
| **xl** | 32px | `p-8`, `gap-8` | Margem entre seções do app |
| **2xl** | 48px | `p-12` | Margem entre seções da landing page |
| **3xl** | 64px | `p-16` | Margem grande entre seções da landing page |

### Padrões de Layout

- **Sidebar:** 256px (w-64) fixa
- **Content max-width:** 1280px (max-w-7xl) no app, 1152px (max-w-6xl) na landing
- **Card padding:** 16px (p-4) para cards compactos, 24px (p-6) para cards grandes
- **Gap entre cards em grid:** 16px (gap-4) ou 24px (gap-6)
- **Margem lateral mobile:** 16px (px-4)
- **Margem lateral desktop:** 32px (px-8) ou auto com max-width

---

## Border Radius

| Token | Valor | Tailwind | Uso |
|-------|-------|----------|-----|
| **sm** | 4px | `rounded-xs` | Badges, tags, chips |
| **base** | 6px | `rounded-sm` | Inputs, selects, small buttons |
| **md** | 8px | `rounded-md` | Botões, dropdowns, tooltips |
| **lg** | 12px | `rounded-lg` | Cards, modals, panels |
| **xl** | 16px | `rounded-xl` | Cards grandes, hero screenshots |
| **full** | 9999px | `rounded-full` | Avatares, pills, toggles |

**Padrão geral:** `rounded-lg` (12px) para cards e containers, `rounded-md` (8px) para botões e inputs.

---

## Sombras

| Token | Valor | Tailwind | Uso |
|-------|-------|----------|-----|
| **None** | — | `shadow-none` | Estado padrão de cards (usar border ao invés de sombra) |
| **sm** | `0 1px 2px rgba(0,0,0,0.05)` | `shadow-xs` | Hover em cards, dropdowns |
| **base** | `0 1px 3px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.06)` | `shadow-sm` | Cards elevados, popovers |
| **md** | `0 4px 6px rgba(0,0,0,0.07), 0 2px 4px rgba(0,0,0,0.06)` | `shadow-md` | Modals, screenshots na landing |
| **lg** | `0 10px 15px rgba(0,0,0,0.1), 0 4px 6px rgba(0,0,0,0.05)` | `shadow-lg` | Elementos flutuantes, hero screenshot |

**Filosofia:** Preferir borders sobre sombras no app. Sombras usadas principalmente na landing page e em elementos flutuantes (modals, dropdowns, tooltips).

---

## Componentes PrimeNG — Diretrizes de Uso

### Quais usar

| Componente PrimeNG | Onde usar no FinDash |
|--------------------|---------------------|
| **Table (p-table)** | Listagem de contas a pagar/receber, fornecedores, clientes, lançamentos importados |
| **Calendar (p-calendar)** | Seletor de data em formulários, filtro de período |
| **Dropdown (p-dropdown)** | Seletor de empresa, seletor de categoria/fornecedor nos filtros |
| **InputText** | Campos de texto em formulários |
| **InputNumber** | Campos de valor monetário |
| **Button** | Ações primárias e secundárias |
| **Dialog (p-dialog)** | Modals de confirmação, formulários rápidos |
| **Toast (p-toast)** | Notificações de sucesso/erro |
| **FileUpload (p-fileUpload)** | Upload de OFX/CSV |
| **Chart (p-chart)** | Gráficos do dashboard (baseado em Chart.js) |
| **Accordion (p-accordion)** | FAQ na landing page |
| **Menu/Menubar** | Navegação e menus de contexto |
| **Tag (p-tag)** | Status de contas (pendente, pago, vencido) |
| **Skeleton (p-skeleton)** | Loading states |
| **ConfirmDialog** | Confirmação de exclusão |

### Customização do PrimeNG com Tailwind

Usar PrimeNG unstyled mode ou aplicar override com Tailwind para manter consistência visual:

- Resetar estilos default do PrimeNG onde conflitarem com o design system
- Aplicar cores, border-radius e espaçamento do design system via Tailwind
- Manter a funcionalidade do PrimeNG (acessibilidade, keyboard nav) intacta
- Preferir componentes nativos HTML + Tailwind para elementos simples (botões, inputs básicos)
- Usar PrimeNG para componentes complexos (table, calendar, file upload, charts)

---

## Ícones

**Biblioteca:** Lucide Icons (lucide.dev) ou PrimeIcons (incluído no PrimeNG)

**Regras:**
- Tamanho padrão: 20px (w-5 h-5) para ícones inline
- Tamanho em cards: 24px (w-6 h-6)
- Cor: `text-gray-500` por padrão, `text-primary` quando ativo
- Ícones monocromáticos sempre
- Stroke width consistente

---

## Estados Interativos

| Estado | Visual |
|--------|--------|
| **Default** | Background white, border gray-200 |
| **Hover** | Background gray-50 ou border gray-300 |
| **Active/Selected** | Background blue-50, border blue-500, text blue-700 |
| **Disabled** | Opacity 50%, cursor not-allowed |
| **Error** | Border red-500, text red-600, background red-50 |
| **Loading** | Skeleton shimmer ou spinner |

---

## Padrões de Status Financeiro

| Status | Cor | Tag Style |
|--------|-----|-----------|
| **Pendente** | `amber-600` em `amber-50` | Tag amarela |
| **Pago / Recebido** | `emerald-600` em `emerald-50` | Tag verde |
| **Vencido** | `red-600` em `red-50` | Tag vermelha |
| **Parcial** | `blue-600` em `blue-50` | Tag azul |

---

## Formatação de Valores Financeiros

- Moeda: `R$ 1.234,56` (formato brasileiro)
- Valores positivos: cor `emerald-600`
- Valores negativos: cor `red-600`, prefixo `-`
- Font: monospace (JetBrains Mono) para alinhamento visual em tabelas
- Totalizadores grandes no dashboard: `text-2xl font-bold font-mono`

---

## Referências Visuais

| Referência | URL | O que observar |
|------------|-----|----------------|
| Linear | linear.app | Layout clean, tipografia, uso de cor mínima, sidebar |
| Resend | resend.com | Simplicidade, whitespace, tables |
| Vercel | vercel.com | Gradientes sutis, hierarquia visual |
| Clerk | clerk.com | Light mode, friendly, componentes |
| Mercury (banking) | mercury.com | Dashboard financeiro clean, gráficos |
| Brex | brex.com | Dashboard financeiro, cards de resumo |

---

## Notas

- Light mode only no MVP. Dark mode é futuro.
- Desktop-first. Responsivo mas não otimizado para mobile.
- Acessibilidade WCAG AA obrigatória: contraste, focus visible, ARIA labels.
- Animações sutis: transições de 150-200ms, ease-out. Sem animações chamativas.
- Manter consistência: se um card usa `rounded-lg` e `p-6`, todos os cards do mesmo tipo devem usar.
