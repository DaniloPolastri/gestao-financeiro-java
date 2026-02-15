# Estrutura Backend

Estrutura do backend do projeto

src/main/java/com/seuprojeto
│
├── SeuprojetoApplication.java
│
├── config/                # Configurações
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   └── CorsConfig.java
│
├── controller/            # Camada REST (entrada)
│   └── UsuarioController.java
│
├── service/               # Regras de negócio
│   ├── UsuarioService.java
│   └── impl/
│       └── UsuarioServiceImpl.java
│
├── repository/            # Acesso ao banco
│   └── UsuarioRepository.java
│
├── entity/                # Entidades JPA
│   └── Usuario.java
│
├── dto/                   # Objetos de transferência
│   ├── UsuarioRequestDTO.java
│   └── UsuarioResponseDTO.java
│
├── mapper/                # Conversão DTO ↔ Entity
│   └── UsuarioMapper.java
│
├── exception/             # Tratamento de erros
│   ├── GlobalExceptionHandler.java
│   └── BusinessException.java
│
├── util/                  # Classes utilitárias
│   └── DateUtils.java
│
└── security/              # JWT, filtros, etc
    ├── JwtService.java
    └── JwtFilter.java


---

# Exemplos de uso:

# Controller 

```java
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(
            @RequestBody UsuarioRequestDTO dto) {

        return ResponseEntity.ok(service.criar(dto));
    }
}
```

---

# Service

```java
public interface UsuarioService {

    UsuarioResponseDTO criar(UsuarioRequestDTO dto);

}
```

```java
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repository;

    private final UsuarioMapper mapper;

    public UsuarioServiceImpl(
            UsuarioRepository repository,
            UsuarioMapper mapper) {

        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UsuarioResponseDTO criar(UsuarioRequestDTO dto){

        // Converte DTO → Entity
        Usuario usuario = mapper.toEntity(dto);

        // Salva no banco
        repository.save(usuario);

        // Converte Entity → DTO
        return mapper.toDTO(usuario);
    }
}
```

---

# Repository

```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

}
```

---

# Entity

```java
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String email;

}
```

---

# DTO

```java
public class UsuarioRequestDTO {

    private String nome;

    private String email;

}
```

```java
public class UsuarioResponseDTO {

    private Long id;

    private String nome;

    private String email;

}
```

---

# Mapper

```java
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    // DTO → Entity
    Usuario toEntity(UsuarioRequestDTO dto);

    // Entity → DTO
    UsuarioResponseDTO toDTO(Usuario entity);

}
```

---

# Fluxo 

Controller
   ↓
Service
   ↓
Mapper (DTO → Entity)
   ↓
Repository
   ↓
Banco
   ↓
Repository
   ↓
Mapper (Entity → DTO)
   ↓
Service
   ↓
Controller
   ↓
JSON resposta

---

# Dependência MapStruct (pom.xml)

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>

<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>
```

---

# Benefícios

• Código limpo  
• Separação de responsabilidades  
• Sem conversão manual  
• Melhor performance  
• Arquitetura profissional  

---

# Padrão usado em produção

• Bancos  
• Fintechs  
• SaaS  
• Microsserviços  
• Sistemas enterprise  

