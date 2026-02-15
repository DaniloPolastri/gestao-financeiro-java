# Estrutura Frontend Angular 21

Estrutura profissional recomendada para projetos Angular 21

Baseada em:

• Standalone Components (padrão novo)  
• Arquitetura por domínio (feature-based)  
• Escalável  
• Enterprise ready  

---

# Estrutura

src/
│
├── main.ts
│
├── app/
│   │
│   ├── app.config.ts
│   ├── app.routes.ts
│   └── app.component.ts
│
│
├── core/                         # Singleton, global, uma única instância
│   │
│   ├── services/
│   │   ├── api.service.ts
│   │   ├── auth.service.ts
│   │   └── storage.service.ts
│   │
│   ├── guards/
│   │   └── auth.guard.ts
│   │
│   ├── interceptors/
│   │   └── auth.interceptor.ts
│   │
│   └── models/
│       └── usuario.model.ts
│
│
├── shared/                      # Componentes reutilizáveis
│   │
│   ├── components/
│   │   ├── navbar/
│   │   ├── button/
│   │   └── input/
│   │
│   ├── directives/
│   │
│   ├── pipes/
│   │
│   └── utils/
│
│
├── features/                   # Domínios do sistema ⭐
│   │
│   ├── auth/
│   │   │
│   │   ├── pages/
│   │   │   ├── login/
│   │   │   │   ├── login.component.ts
│   │   │   │   ├── login.component.html
│   │   │   │   └── login.component.css
│   │   │   │
│   │   │   └── register/
│   │   │
│   │   ├── services/
│   │   │   └── auth-api.service.ts
│   │   │
│   │   ├── models/
│   │   │
│   │   └── auth.routes.ts
│   │
│   │
│   └── usuario/
│       │
│       ├── pages/
│       │   ├── usuario-list/
│       │   └── usuario-form/
│       │
│       ├── services/
│       │   └── usuario-api.service.ts
│       │
│       ├── models/
│       │   └── usuario.ts
│       │
│       └── usuario.routes.ts
│
│
├── layout/                     # Layout da aplicação
│   │
│   ├── main-layout/
│   │   ├── main-layout.component.ts
│   │   ├── main-layout.component.html
│   │   └── main-layout.component.css
│
│
├── environments/
│   ├── environment.ts
│   └── environment.prod.ts
│
│
└── assets/

---

# Explicação das pastas

---

# core/

Serviços globais

Singleton

Exemplo:

• AuthService  
• ApiService  
• Interceptor JWT  

Nunca depende de features

---

# shared/

Componentes reutilizáveis

Exemplo:

• ButtonComponent  
• InputComponent  
• ModalComponent  

Pode ser usado em qualquer lugar

---

# features/

Domínios do sistema ⭐

Exemplo:

• auth
• usuario
• financeiro
• dashboard

Cada feature é independente

Melhor prática atual Angular

---

# layout/

Layout da aplicação

Exemplo:

• Sidebar
• Navbar
• Footer

---

# models/

Interfaces Typescript

Exemplo:

```ts
export interface Usuario {

  id: number;

  nome: string;

  email: string;

}
```

---

# services/

Comunicação com backend

Exemplo:

```ts
@Injectable({
  providedIn: 'root'
})
export class UsuarioApiService {

  private api = inject(HttpClient);

  private baseUrl = 'http://localhost:8080/usuarios';

  listar() {

    return this.api.get<Usuario[]>(this.baseUrl);

  }

  criar(usuario: Usuario) {

    return this.
