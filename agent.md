# Guia de Desenvolvimento Backend ARGOS

## 1. Visão geral

O ARGOS é um backend Java responsável por apoiar o controle operacional de propriedades rurais, medicamentos, lotes, aplicações, estoque, alertas e integrações externas.

Este guia define padrões de arquitetura, nomenclatura, segurança, tratamento de erros, testes, persistência e observabilidade. O objetivo é manter o código limpo, testável, consistente e escalável.

O escopo cobre o backend Java, suas camadas, integrações, acesso ao PostgreSQL e práticas de qualidade. Não cobre contratos de endpoints específicos, telas mobile, regras de negócio detalhadas ou a implementação de procedures/functions reais.

## 2. Estrutura de projeto

O pacote-base do projeto é `com.argos.backend_services`.

| Camada | Responsabilidade |
|---|---|
| `controller` | Receber requisições HTTP, validar entrada e devolver respostas. |
| `service` | Centralizar regras de negócio e coordenar operações. |
| `repository` | Encapsular persistência, queries e chamadas ao banco. |
| `entity` | Representar tabelas e relacionamentos JPA. |
| `dto.in` / `dto.out` | Definir contratos de entrada e saída sem expor entidades. |
| `mapper` | Converter DTOs e entidades, preferencialmente com MapStruct. |
| `exception` | Conter exceções de domínio e infraestrutura. |
| `controller.handler` | Converter exceções em respostas HTTP uniformes. |
| `adapter` | Isolar integrações com APIs, serviços e formatos externos. |
| `security` | Validar JWT, montar o contexto do usuário e aplicar autorização. |
| `config` | Configurações de Spring, segurança, Firebase e integrações. |
| `resources/db/migration` | Migrações versionadas do banco via Flyway. |

Fluxo padrão:

```text
HTTP -> Controller -> Service -> Repository -> PostgreSQL
                         |             |
                         v             v
                       Mapper       Adapter/rotina do banco
```

Controllers não devem conter regras de negócio. Services não devem montar respostas HTTP. Repositories não devem decidir regras de autorização ou fluxo de apresentação.

## 3. Padrões de nomenclatura

| Item | Convenção | Exemplo |
|---|---|---|
| Controller | `[Recurso]Controller` | `MedicineController` |
| Service | `[Recurso]Service` | `MedicineService` |
| Entity | Nome singular do recurso | `Medicine` |
| Request DTO | `[Recurso]In` ou `[Recurso]Request` | `MedicineIn` |
| Response DTO | `[Recurso]Out` ou `[Recurso]Response` | `MedicineOut` |
| Mapper | `[Recurso]Mapper` | `MedicineMapper` |
| Repository | `[Recurso]Repository` | `MedicineRepository` |
| Exceção | Condição + `Exception` | `MedicineNotFoundException` |
| Adapter | Serviço externo + `Adapter` | `ExternalCatalogAdapter` |
| Teste | Classe + `Test` | `MedicineServiceTest` |

- Classes e interfaces usam `PascalCase`.
- Métodos e variáveis usam `camelCase`.
- Constantes usam `UPPER_SNAKE_CASE`.
- Métodos de consulta usam verbos claros: `findBy`, `existsBy`, `countBy`, `list`, `create`, `update` e `delete`.
- Pacotes usam letras minúsculas, sem abreviações desnecessárias.
- O nome deve expressar intenção; evite `Helper`, `Manager` e `Util` genéricos.

## 4. Padrão de requisições

A autenticação deve usar o header padrão:

```http
Authorization: Bearer <jwt>
```

O filtro de segurança valida o token e extrai a identidade do usuário e suas claims. O `userId` deve ser obtido do contexto autenticado sempre que possível. Controllers que ainda recebem `userId` explicitamente devem validar sua presença e consistência com o usuário autenticado.

Não introduza `consumer_id` como nova convenção. Compatibilidade com código legado deve ser tratada de forma explícita e temporária.

Status esperados:

| Status | Uso |
|---:|---|
| 200 | Operação concluída com resposta. |
| 201 | Recurso criado. |
| 400 | Entrada inválida ou regra de negócio violada. |
| 401 | Token ausente, inválido ou usuário não autenticado. |
| 403 | Usuário autenticado sem permissão. |
| 404 | Recurso não encontrado. |
| 500 | Erro inesperado não mapeado. |

Valide entradas com Jakarta Validation e `@Valid`. Nunca confie em um identificador vindo do corpo quando ele puder ser obtido do JWT.

## 5. Tratamento de erros

Todas as exceções de domínio devem derivar de uma base ARGOS, como `ArgosException`. Exceções específicas devem representar condições conhecidas, por exemplo `MedicineNotFoundException`, `UnauthorizedException` e `IntegrationException`.

A resposta de erro deve ser uniforme:

```json
{
  "information_code": "MEDICINE_NOT_FOUND",
  "message": "Medicine was not found for the requested identifier",
  "httpStatus": 404
}
```

`information_code` é estável, curto e adequado para suporte. `message` explica o contexto sem incluir token, senha ou dados pessoais.

```java
public class ResourceNotFoundException extends ArgosException {

    public ResourceNotFoundException(String informationCode, String message) {
        super(informationCode, message, HttpStatus.NOT_FOUND);
    }
}
```

O handler global deve mapear exceções conhecidas e converter falhas desconhecidas em resposta 500 por meio de `CustomGlobalException` ou componente equivalente. Não exponha stack trace ao cliente.

Registre exceções com contexto técnico suficiente: operação, recurso, identificador interno não sensível, correlation id e causa. Não registre tokens, senhas ou payloads sensíveis.

## 6. Testes unitários e cobertura superior a 90%

Cobertura global acima de 90% é requisito de qualidade, não sugestão. A cobertura deve ser medida com JaCoCo e analisada por pacote, não apenas pelo número global.

| Camada | Meta mínima | Estratégia |
|---|---:|---|
| Controller | 85% | Mock do Service e validação de status, entrada e segurança. |
| Service | 95% | Mockito para dependências e cobertura de caminhos de negócio. |
| Repository | 80% | `@DataJpaTest` e banco de teste compatível. |
| Mapper | 90% | Testes determinísticos de ida, volta e valores nulos permitidos. |
| Global | >90% | JaCoCo no build e revisão de linhas não cobertas. |

Use JUnit 5, Mockito e AssertJ. Todo teste deve seguir AAA:

```java
@Test
void shouldReturnMedicineWhenItExists() {
    // Arrange
    var medicine = new Medicine();
    when(repository.findById(ID)).thenReturn(Optional.of(medicine));

    // Act
    var result = service.findById(ID);

    // Assert
    assertThat(result).isNotNull();
    verify(repository).findById(ID);
}
```

Teste sucesso, validação, ausência de dados, autorização, falhas de dependência e conversão de exceções. Testes não devem depender da ordem de execução nem de dados externos instáveis.

Exemplos mínimos por camada:

```java
// Controller: use MockMvc ou chamada direta e verifique status e interação com o Service.
verify(service).create(any(MedicineIn.class), eq(userId));

// Service: valide a regra e a exceção esperada.
assertThatThrownBy(() -> service.findById(ID))
        .isInstanceOf(ResourceNotFoundException.class);

// Repository: valide uma query customizada com @DataJpaTest.
assertThat(repository.findByBrandName("Example")).isPresent();

// Mapper: confirme que todos os campos relevantes foram convertidos.
assertThat(mapper.toOut(entity).name()).isEqualTo(entity.getName());
```

## 7. Padrão DTO e mappers

Nunca exponha uma Entity diretamente no controller. Crie DTOs quando houver entrada externa, saída pública, campos sensíveis, validações diferentes ou necessidade de desacoplar o banco da API.

Prefira nomes `[Entity]In`/`[Entity]Out` no padrão atual ou `[Entity]Request`/`[Entity]Response` em novos módulos, mantendo consistência dentro do módulo.

```java
public interface MedicineMapper {
    MedicineOut toOut(Medicine entity);
    Medicine toEntity(MedicineIn input);
}
```

MapStruct é preferível para conversões repetitivas. Conversões com regra de negócio devem permanecer no Service ou em um componente dedicado, não escondidas em um mapper automático.

## 8. Procedures e functions

Use rotinas do banco quando houver cálculo relacional complexo, operação transacional com múltiplas tabelas, necessidade de atomicidade ou lógica que deva ser compartilhada com outros consumidores.

O acesso deve ficar no Repository ou em uma abstração de persistência dedicada:

```java
public interface DatabaseRoutineRepository extends Repository<Medicine, UUID> {

    @Procedure(procedureName = "sp_registrar_aplicacao")
    void registerApplication(
            @Param("p_lot_id") UUID lotId,
            @Param("p_medicine_batch_id") UUID batchId,
            @Param("p_user_id") UUID userId,
            @Param("p_dose") BigDecimal dose);
}
```

Para functions ou queries com retorno, use `@Query` parametrizado ou uma implementação de repository adequada:

```java
@Query(value = "select fn_data_fim_carencia(:medicineId, :applicationDate)", nativeQuery = true)
LocalDate calculateWithdrawalEnd(UUID medicineId, LocalDate applicationDate);
```

Não concatene SQL. Traduza falhas de rotina para uma `ArgosException` específica, preserve a transação quando necessário e teste sucesso, erro e rollback.

## 9. Padrão Controller

Controllers devem ser finos, declarados com `@RestController` e `@RequestMapping`, usar anotações HTTP explícitas e delegar a operação ao Service.

```java
@RestController
@RequestMapping("/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService service;

    @GetMapping("/{id}")
    public ResponseEntity<MedicineOut> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<MedicineOut> create(
            @Valid @RequestBody MedicineIn input,
            @CurrentUser CurrentUserData user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(input, user.id()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicineOut> update(
            @PathVariable UUID id,
            @Valid @RequestBody MedicineIn input,
            @CurrentUser CurrentUserData user) {
        return ResponseEntity.ok(service.update(id, input, user.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @CurrentUser CurrentUserData user) {
        service.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }
}
```

Use `@Valid`, `@NotNull`, `@NotBlank`, `@Positive` e validações equivalentes. Não capture `Exception` genericamente no controller.

## 10. Padrão Service

O Service concentra a lógica de negócio, valida autorização no domínio, coordena repositories e adapters e define limites transacionais.

```java
@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository repository;
    private final MedicineMapper mapper;

    @Transactional(readOnly = true)
    public MedicineOut findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toOut)
                .orElseThrow(() -> new MedicineNotFoundException(id));
    }

    @Transactional
    public MedicineOut create(MedicineIn input, UUID userId) {
        var entity = mapper.toEntity(input);
        entity.setCreatedBy(userId);
        return mapper.toOut(repository.save(entity));
    }
}
```

Use injeção por construtor, preferencialmente com `@RequiredArgsConstructor`. Use `@Transactional` na fronteira da operação. Não use `try-catch` para esconder exceções; capture somente quando puder adicionar contexto, traduzir uma falha ou executar compensação.

## 11. Padrão Repository

Repositories devem estender `JpaRepository<Entity, ID>` quando a persistência for JPA:

```java
public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

    Optional<Medicine> findByBrandName(String brandName);

    boolean existsByRegistrationCode(String registrationCode);

    @Query("select m from Medicine m where lower(m.name) like lower(concat('%', :term, '%'))")
    Page<Medicine> search(@Param("term") String term, Pageable pageable);
}
```

Use métodos derivados para consultas simples e `@Query` parametrizado para consultas expressivas. Toda lista potencialmente grande deve aceitar `Pageable`. Procedures e functions devem ter métodos com nomes de domínio claros e testes correspondentes.

## 12. Padrão Entity

Entities representam persistência, não contratos HTTP:

```java
@Entity
@Table(name = "medicine")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medicine {

    @Id
    @GeneratedValue
    private UUID id;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

Use `@Entity`, `@Table`, `@Id` e `@GeneratedValue` com mapeamentos explícitos. Defina relacionamentos com `@ManyToOne`, `@OneToMany` e `@ManyToMany` somente quando o ciclo de vida estiver claro. Prefira `LAZY` por padrão e evite `EAGER` sem justificativa.

Validações estruturais podem usar Jakarta Validation. Regras que dependem de banco, usuário ou outras entidades pertencem ao Service. Campos de auditoria devem registrar `createdAt`, `updatedAt`, `createdBy` e `updatedBy` quando aplicável.

## 13. Padrão Adapter para APIs externas

Cada integração externa deve ficar em `adapter`, isolando cliente, autenticação, payload externo, timeout e conversão para modelos internos.

```java
@Component
@RequiredArgsConstructor
public class ExternalMedicineAdapter {

    private final ExternalMedicineClient client;

    public MedicineCatalogData findByRegistration(String registration) {
        try {
            return client.find(registration).toDomain();
        } catch (ExternalClientException exception) {
            throw new IntegrationException(
                    "EXTERNAL_MEDICINE_UNAVAILABLE",
                    "External medicine catalog is unavailable",
                    exception);
        }
    }
}
```

O decoder de erro deve converter falhas externas para exceções ARGOS. Use retry somente para falhas transitórias, com limite, backoff e idempotência. Defina timeout e fallback; não bloqueie indefinidamente uma requisição. Registre falhas sem token, credenciais ou payload sensível.

## 14. Boas práticas gerais

- Aplique DRY sem criar abstrações prematuras.
- Aplique SOLID com foco em responsabilidades pequenas e dependências invertidas.
- Prefira objetos imutáveis, records para DTOs simples e métodos puros quando possível.
- Evite `null` em contratos; use `Optional` apenas como retorno de consulta, não como campo de Entity ou parâmetro.
- Mantenha métodos curtos e nomeados por intenção.
- Não use `System.out.println`; use SLF4J.
- Não deixe código comentado, TODO sem responsável ou lógica duplicada.
- Comente decisões e restrições, não repita o que o código já expressa.
- Faça code review verificando segurança, transação, cobertura, observabilidade e compatibilidade.

## 15. Segurança

- Valide `Authorization: Bearer <jwt>` no filtro de segurança.
- Extraia a identidade do token validado; não confie em `userId` arbitrário do corpo.
- Diferencie autenticação, autorização e escopo de propriedade/lote.
- Usuários só podem consultar ou alterar dados autorizados ao seu escopo.
- Use parâmetros em `@Query`, nunca concatene valores em SQL.
- Não registre tokens, senhas, credenciais, documentos pessoais ou payloads sensíveis.
- Mantenha credenciais fora do código e do versionamento.
- Teste token ausente, expirado, inválido, usuário sem permissão e acesso fora do escopo.

## 16. Performance

- Detecte N+1 com `@EntityGraph`, fetch planejado ou query específica.
- Use `Pageable` em listagens.
- Crie índices orientados a consultas reais e confirme impacto com `EXPLAIN ANALYZE`.
- Use cache somente quando a invalidação e a consistência forem compreendidas.
- Prefira `LAZY`; carregue relações explicitamente quando necessárias.
- Evite retornar entidades grandes ou relacionamentos inteiros em uma única resposta.
- Monitore tempo de query, contagem de queries e chamadas externas.

## 17. Logs e monitoramento

Use SLF4J com níveis coerentes:

| Nível | Uso |
|---|---|
| `DEBUG` | Diagnóstico local e detalhes não sensíveis. |
| `INFO` | Operações relevantes concluídas. |
| `WARN` | Condições recuperáveis ou comportamento inesperado. |
| `ERROR` | Falhas que exigem investigação. |

Prefira logs estruturados com operação, recurso, status, duração, correlation id e identificadores internos seguros:

```java
log.info("medicine_created medicineId={} userId={} durationMs={}",
        medicineId, userId, durationMs);
```

Não registre tokens, senhas, dados pessoais completos, credenciais ou respostas integrais de APIs externas. Monitore erros 4xx/5xx, latência, falhas de integração, pool de conexões, filas e execução de rotinas do banco.

## 18. Dependências e versões

O baseline atual do projeto é:

| Tecnologia | Padrão |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.x |
| Spring Data JPA | Versão gerenciada pelo Spring Boot |
| JUnit | JUnit 5 via `spring-boot-starter-test` |
| Mockito | Versão gerenciada pelo Spring Boot |
| AssertJ | Versão gerenciada pelo Spring Boot |
| MapStruct | 1.6.x |
| PostgreSQL | Driver runtime e migrations Flyway |
| Jackson | Serialização JSON pelo Spring Web |
| Jakarta Validation | Validação de entrada e constraints estruturais |
| Lombok | Opcional; use com parcimônia e revisão do código gerado |

Não atualize versões isoladamente sem avaliar compatibilidade, vulnerabilidades, changelog e impacto nos testes.

## 19. Checklist pré-commit

- [ ] Testes unitários e de integração relevantes passam.
- [ ] Cobertura global permanece acima de 90%.
- [ ] Metas por camada são atendidas.
- [ ] Não há warnings novos de compilação.
- [ ] Formatação e convenções do projeto foram seguidas.
- [ ] Não há código comentado, TODO abandonado ou `System.out.println`.
- [ ] Controllers usam o contexto JWT e validam o usuário quando necessário.
- [ ] Exceções são específicas e retornam o formato padrão.
- [ ] Queries são parametrizadas.
- [ ] Não há segredo, token ou dado pessoal em logs.
- [ ] Alterações de banco possuem migration Flyway versionada.
- [ ] Métodos públicos novos possuem JavaDoc quando sua intenção não for óbvia.
- [ ] O code review avaliou transação, autorização, performance e observabilidade.

## 20. Exemplo prático genérico de Medicamento

O exemplo abaixo demonstra a composição das camadas sem reproduzir contratos reais do sistema.

### DTOs

```java
public record MedicineIn(
        @NotBlank String name,
        @NotBlank String registrationCode,
        @Positive BigDecimal standardDose,
        @NotBlank String unit) {
}

public record MedicineOut(
        UUID id,
        String name,
        String registrationCode,
        BigDecimal standardDose,
        String unit) {
}
```

### Mapper

```java
@Mapper(componentModel = "spring")
public interface MedicineMapper {
    MedicineOut toOut(Medicine entity);
    Medicine toEntity(MedicineIn input);
}
```

### Repository

```java
public interface MedicineRepository extends JpaRepository<Medicine, UUID> {
    Optional<Medicine> findByRegistrationCode(String registrationCode);
    Page<Medicine> findAllByNameContainingIgnoreCase(String name, Pageable pageable);
}
```

### Service

```java
@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository repository;
    private final MedicineMapper mapper;

    @Transactional(readOnly = true)
    public Page<MedicineOut> list(String name, Pageable pageable) {
        return repository.findAllByNameContainingIgnoreCase(name, pageable)
                .map(mapper::toOut);
    }

    @Transactional(readOnly = true)
    public MedicineOut find(UUID id) {
        return repository.findById(id)
                .map(mapper::toOut)
                .orElseThrow(() -> new MedicineNotFoundException(id));
    }

    @Transactional
    public MedicineOut create(MedicineIn input, UUID userId) {
        if (repository.findByRegistrationCode(input.registrationCode()).isPresent()) {
            throw new ArgosBadRequestException("MEDICINE_ALREADY_EXISTS",
                    "A medicine with this registration already exists");
        }
        return mapper.toOut(repository.save(mapper.toEntity(input)));
    }

    @Transactional
    public MedicineOut update(UUID id, MedicineIn input, UUID userId) {
        var medicine = repository.findById(id)
                .orElseThrow(() -> new MedicineNotFoundException(id));
        medicine.updateFrom(input, userId);
        return mapper.toOut(repository.save(medicine));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        var medicine = repository.findById(id)
                .orElseThrow(() -> new MedicineNotFoundException(id));
        medicine.deactivate(userId);
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService service;

    @GetMapping
    public Page<MedicineOut> list(
            @RequestParam(defaultValue = "") String name,
            Pageable pageable) {
        return service.list(name, pageable);
    }

    @GetMapping("/{id}")
    public MedicineOut find(@PathVariable UUID id) {
        return service.find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicineOut create(
            @Valid @RequestBody MedicineIn input,
            @CurrentUser CurrentUserData user) {
        return service.create(input, user.id());
    }

    @PutMapping("/{id}")
    public MedicineOut update(
            @PathVariable UUID id,
            @Valid @RequestBody MedicineIn input,
            @CurrentUser CurrentUserData user) {
        return service.update(id, input, user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @CurrentUser CurrentUserData user) {
        service.delete(id, user.id());
    }
}
```

### Teste de Service

```java
@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock MedicineRepository repository;
    @Mock MedicineMapper mapper;
    @InjectMocks MedicineService service;

    @Test
    void shouldFindMedicine() {
        var entity = new Medicine();
        var output = new MedicineOut(UUID.randomUUID(), "Example", "R-1",
                BigDecimal.ONE, "ML");
        when(repository.findById(any())).thenReturn(Optional.of(entity));
        when(mapper.toOut(entity)).thenReturn(output);

        var result = service.find(UUID.randomUUID());

        assertThat(result).isEqualTo(output);
        verify(repository).findById(any());
    }

    @Test
    void shouldFailWhenMedicineDoesNotExist() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.find(UUID.randomUUID()))
                .isInstanceOf(MedicineNotFoundException.class);
    }
}
```

O conjunto real de testes deve complementar esse exemplo com criação, atualização, exclusão, duplicidade, validação, autorização, falhas de persistência e cobertura do handler global. O objetivo é manter cobertura superior a 90% com testes significativos, não apenas executar linhas sem verificar comportamento.
