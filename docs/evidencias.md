# Evidências dos requisitos

## Matriz consolidada dos requisitos (01 a 16)

Cada linha aponta o responsável, o código correspondente na base integrada e a requisição da
coleção Postman ou o teste automatizado que comprova o requisito. Os caminhos Java abaixo são
relativos a `src/main/java/br/com/equipe/eventos/`, salvo indicação.

| # | Requisito | Responsável | Código | Requisição Postman / teste | Status |
|---|-----------|-------------|--------|----------------------------|--------|
| 01 | Recursos e métodos HTTP | Felipe | `evento/boundary/EventoResource.java`, `EventoEnderecoResource.java` | Postman "01-02-12-13 CRUD" (`GET /eventos`, `GET /eventos/{id}`, `POST`, `PUT`, `DELETE`; inscrição como subrecurso `POST /eventos/{id}/inscricoes`, sem verbo na URI); `EventoResourceTest` | ✅ |
| 02 | Códigos de status e contratos | Felipe / Gean | `evento/boundary/EventoResource.java`, `platform/erro/` | Postman "01-02-12-13 CRUD" e "02 Limpeza CRUD": 200 (consulta), 201 + `Location` (Criar evento), 204 sem corpo (Remover evento), 400/404/409 padronizados; `EventoResourceTest` e `ErroRespostaTest` | ✅ |
| 03 | Lombok aplicado conscientemente | Gean | `evento/repository/Evento.java`, `platform/erro/ErroResposta.java`, `pom.xml` | `EventoRepositoryTest`, builders usados pelos mappers em `ErroRespostaTest`; explicação técnica na seção 03 abaixo | ✅ |
| 04 | API Quarkus/JAX-RS | Felipe | `evento/boundary/EventoResource.java` | `@Path("/eventos")`, `@GET`/`@POST`/`@PUT`/`@DELETE`, `@Produces`/`@Consumes` JSON; Postman "Listar eventos - 200"; `EventoResourceTest.listarResponde200ComOsEventos` | ✅ |
| 05 | CDI e regra de negócio | Felipe | `evento/domain/EventoService.java` | `EventoService` `@ApplicationScoped` injetado por construtor no resource. Regras: `EVENTO_LOTADO` e `CAPACIDADE_ABAIXO_DOS_INSCRITOS`. Postman "05-10 Regra de negocio e contador"; `EventoServiceTest.inscreverEmEventoLotadoLancaEventoLotado`, `atualizarCapacidadeAbaixoDosInscritosLancaRegraDeNegocio` | ✅ |
| 06 | Consumo de API externa | Romulo | `integracao/enderecos/EnderecoClient.java`, `EventoEnderecoResource.java` | `@RegisterRestClient(configKey = "enderecos-api")`; Postman "06-09 Externo UP e DOWN" (`GET /eventos/{id}/endereco`); `EnderecoResourceTest` (mock) | ✅ |
| 07 | Configurações e profiles | Romulo | `src/main/resources/application.properties` | `eventos.app` (base) e `%dev.eventos.app`, `eventos.manutencao`, H2 `%dev`/`%prod` e sobrescrita por variável de ambiente; Postman "07 Profiles dev e prod" | ✅ |
| 08 | Liveness e readiness | Romulo | `platform/health/AplicacaoLivenessCheck.java`, `ProntidaoReadinessCheck.java`, `ReadinessService.java`, `evento/boundary/ReadinessResource.java` | Postman "08 Health UP e DOWN" (`GET /q/health/live`, `GET /ready/up`, `GET /ready/down`, `GET /q/health/ready`) | ✅ |
| 09 | Tolerância a falhas | Romulo | `integracao/enderecos/EnderecoService.java` | `@Retry`, `@ExponentialBackoff`, `@Timeout` e `@Fallback` em `EnderecoService.buscar`; Postman "06-09 Externo UP e DOWN" | ✅ |
| 10 | Métrica de negócio | Romulo | `platform/metrics/EventoMetricas.java`, `evento/domain/EventoService.java` | `EventoMetricas.registrarInscricao()` chamado em `EventoService.inscrever`; Postman "05-10 Regra de negocio e contador" (`GET /q/metrics` `eventos_inscricoes_total`) | ✅ |
| 11 | Entidade JPA | Base | `evento/repository/Evento.java` | `EventoRepositoryTest.persisteEConsultaEvento` | ✅ |
| 12 | CRUD Panache Repository | Felipe | `evento/repository/EventoRepository.java`, `evento/domain/EventoService.java` | `listAll`, `findByIdOptional`, `persist`, `delete` do `PanacheRepository`; Postman "01-02-12-13 CRUD"; `EventoServiceTest` (criar, atualizar, remover) | ✅ |
| 13 | Transações e atualização | Felipe | `evento/domain/EventoService.java` | `@Transactional` em criar/atualizar/remover/inscrever; Postman "01-02-12-13 CRUD" (PUT → GET); `EventoResourceTest.putAlteraODadoConsultadoDepois` | ✅ |
| 14 | Bean Validation | Gean | `evento/boundary/EventoRequest.java`, `EventoResource.java` | `@NotBlank`, `@Size`, `@Pattern`, `@NotNull`, `@Future`, `@Positive` e `@Valid` no POST/PUT; Postman "14-15-16 Erros e validacao"; `ErroRespostaTest` | ✅ |
| 15 | ExceptionMapper personalizado | Gean | `platform/erro/ValidacaoMapper.java`, `EventoNaoEncontradoMapper.java`, `RegraDeNegocioMapper.java`, `ErrosTecnicosMapper.java` | Três mappers `@Provider` de domínio e handlers técnicos sem try/catch nos endpoints; Postman "14-15-16 Erros e validacao"; `ErroRespostaTest` | ✅ |
| 16 | Resposta de erro padronizada | Gean | `platform/erro/ErroResposta.java` e mappers | JSON padronizado (`codigo`, `mensagem`, `timestamp`, `campos`) verificado em `ErroRespostaTest` e no script global da coleção Postman | ✅ |

## 03 — Lombok e JPA

Em `Evento`, `@Getter` e `@Setter` geram acessores para os campos privados, e
`@NoArgsConstructor` gera o construtor sem argumentos exigido pelo JPA. O identificador é
gerado pelo banco; `inscritos` inicia em zero pelo valor padrão de `int`. O repository usa a
entidade; o contrato HTTP usa records separados (`EventoRequest`/`EventoResponse`).

**Não usamos `@Data` na entidade.** Além dos acessores, ela geraria `equals`, `hashCode` e
`toString` com base nos campos. Campos mutáveis e o id atribuído no `persist` poderiam mudar o
hash de um objeto já colocado em um `HashSet`/`HashMap`. Em entidades com relacionamentos lazy,
`toString`/comparações automáticas também podem disparar consultas, carregar proxies ou criar
recursão. A entidade atual não tem esses relacionamentos; trata-se de evitar uma escolha
inadequada para sua evolução, não de afirmar que já existe lazy loading nela. Não é necessário
introduzir uma estratégia de igualdade de entidade para este escopo.

`ErroResposta` é um **record**, não uma entidade. O Java já fornece construtor canônico,
acessores (`codigo()`, etc.), `equals`, `hashCode` e `toString`. O Lombok `@Builder` acrescenta
`ErroResposta.builder().codigo(...).mensagem(...).timestamp(...).campos(...).build()`.
Os campos nomeados deixam a montagem do contrato legível; não precisamos de `@Data`, setters
ou construtor vazio. O annotation processor está configurado no `maven-compiler-plugin`, e
Lombok tem escopo `provided`: não é uma dependência de runtime.

## 14 — Validação de entrada

| Componente | Restrições | Contrato |
|------------|------------|----------|
| `nome` | `@NotBlank`, `@Size(max = 120)` | Obrigatório, não apenas espaços, até 120 caracteres |
| `descricao` | `@Size(max = 500)` | Opcional (`null` aceito), até 500 caracteres |
| `cep` | `@NotBlank`, `@Pattern(regexp = "[0-9]{8}")` | Oito dígitos ASCII, sem hífen |
| `dataHora` | `@NotNull`, `@Future` | Obrigatória e futura |
| `capacidade` | `@NotNull`, `@Positive` | Inteiro obrigatório maior que zero |

`@Future`, `@Positive`, `@Size` e `@Pattern` não rejeitam `null` sozinhas; por isso os campos
obrigatórios têm também `@NotNull`/`@NotBlank`. Mensagens não reproduzem valores recebidos.
Os testes geram datas relativas ao relógio, sem depender da validade dos exemplos de `import.sql`.

`EventoResource` usa `@Valid EventoRequest` em POST e PUT antes de delegar ao `EventoService`.
Além disso, `ErroResource` vive exclusivamente em `src/test/java`, em `/teste-erros`, para
exercitar isoladamente todas as combinações de validação e mappers via HTTP sem ser incluído no
artefato de produção.

## 15–16 — Status e contrato de erros

Todos os erros tratados pelo Quarkus REST usam JSON com as mesmas quatro propriedades:

```json
{
  "codigo": "REQUISICAO_INVALIDA",
  "mensagem": "Existem campos invalidos na requisicao.",
  "timestamp": "2026-10-30T17:00:00-03:00",
  "campos": ["cep: CEP deve conter exatamente 8 digitos"]
}
```

| Origem | Status | `codigo` | `campos` |
|--------|--------|----------|----------|
| `ConstraintViolationException` | 400 | `REQUISICAO_INVALIDA` | Lista ordenada e sem duplicatas: `campo: mensagem` |
| `EventoNaoEncontradoException` | 404 | `EVENTO_NAO_ENCONTRADO` | `[]` |
| `RegraDeNegocioException` | 409 | Código original, como `EVENTO_LOTADO` | `[]` |
| JSON malformado/tipo incompatível | 400 | `REQUISICAO_INVALIDA` | `[]` |
| Erro HTTP do framework | Status original (ex.: 404/405/415) | `HTTP_<status>`; 400 usa `REQUISICAO_INVALIDA` | `[]` |
| Exceção inesperada | 500 | `ERRO_INTERNO` | `[]` |

Os três mappers obrigatórios são `@Provider`/`ExceptionMapper`; não há try/catch nos endpoints.
`ErrosTecnicosMapper` usa `@ServerExceptionMapper` do Quarkus REST também para erros fora do
domínio. O handler de `MismatchedInputException` tem prioridade sobre o padrão do Jackson, para
não voltar a outro formato. Falhas 500 são registradas com stack trace no servidor; a resposta
não expõe mensagens internas. Cabeçalhos existentes de exceções HTTP são preservados.
O contrato aplica-se à API REST, não ao mock separado, às respostas de Health/Prometheus,
ou a falhas de transporte que não chegam ao processamento REST.

O teste verifica as quatro chaves exatas, `Content-Type`, mensagens/códigos e timestamp
parseável como `OffsetDateTime`; os casos 404 e 409 exigem `campos: []`. Há cenários de
POST e PUT inválidos, campos ausentes, tamanhos limite, JSON sintaticamente inválido,
tipos incompatíveis, rota inexistente, 405, 415 e 500 sem vazamento de detalhe interno.

## 07 — Configurações e profiles

| Chave | dev/test | prod | Variável de ambiente |
|-------|----------|------|----------------------|
| `quarkus.datasource.jdbc.url` | `jdbc:h2:mem:eventos;DB_CLOSE_DELAY=-1` | `jdbc:h2:file:./data/eventos;DB_CLOSE_DELAY=-1` | `QUARKUS_DATASOURCE_JDBC_URL` |
| `quarkus.hibernate-orm.schema-management.strategy` | `drop-and-create` | `update` | `QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY` |
| `quarkus.http.port` | 8080 | 8080 | `QUARKUS_HTTP_PORT` |
| `quarkus.rest-client.enderecos-api.url` | `http://localhost:8081` | `http://localhost:8081` | `QUARKUS_REST_CLIENT_ENDERECOS_API_URL` |
| `eventos.app` | `API Eventos - DEV` (`%dev`) | `API Eventos` (valor base) | `EVENTOS_APP` |
| `eventos.manutencao` | `false` | `false` (`true` = readiness DOWN) | `EVENTOS_MANUTENCAO` |
| `mock.falhar` (outro app) | `false` | `false` | `MOCK_FALHAR` |

`quarkus:dev` ativa `dev`; `test` ativa `test` e usa porta aleatória; o jar empacotado ativa
`prod`. Em PowerShell, na raiz, usando JDK 21:

```powershell
.\mvnw.cmd quarkus:dev
# Em outro terminal:
.\mvnw.cmd package
$env:QUARKUS_HTTP_PORT = "8082"
$env:QUARKUS_DATASOURCE_JDBC_URL = "jdbc:h2:file:./data/demonstracao;DB_CLOSE_DELAY=-1"
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\quarkus-app\quarkus-run.jar
```

Confirme `Profile dev activated`/`Profile prod activated` no log. A comprovação de persistência
em `prod` é: criar evento, anotar id, reiniciar o mesmo jar com o mesmo arquivo H2 e consultar o id.
Em `dev` o banco é recriado. Remova variáveis no terminal ao terminar
(`Remove-Item Env:\QUARKUS_HTTP_PORT`, etc.).

## 08 — Health e condições DOWN

- **Liveness (`/q/health/live`)**: `AplicacaoLivenessCheck` (`@Liveness`) injeta `@ConfigProperty(name = "eventos.app")` e responde `UP` enquanto o processo está ativo. Fica `DOWN`/indisponível apenas se o processo travar ou encerrar.
- **Readiness (`/q/health/ready`)**: `ProntidaoReadinessCheck` (`@Readiness`) consulta `ReadinessService` além do check automático do datasource H2. Para alternar entre `UP` (200) e `DOWN` (503) em demonstração sem derrubar o processo, chame `GET /ready/up` e `GET /ready/down` (`ReadinessResource`). A readiness começa `UP`; com `EVENTOS_MANUTENCAO=true` ela já sobe `DOWN` (manutenção) enquanto a liveness segue `UP`. Readiness também fica `DOWN` caso a verificação do banco H2 falhe.

## 06 e 09 — Externo UP/DOWN

O mock existente atende `GET /enderecos/{cep}` na porta 8081. CEP conhecido, como `01310100`,
retorna 200 com endereço; CEP desconhecido retorna 404 sem corpo. `MOCK_FALHAR=true` retorna
503 em toda consulta. As respostas do mock não usam `ErroResposta`, pois ele simula outro sistema.

```powershell
# UP:
.\mvnw.cmd -f .\mock-enderecos\pom.xml quarkus:dev
# Para DOWN, encerre o mock e reinicie no mesmo terminal:
$env:MOCK_FALHAR = "true"
.\mvnw.cmd -f .\mock-enderecos\pom.xml quarkus:dev
# Depois de encerrar:
Remove-Item Env:\MOCK_FALHAR
```

Na coleção, execute “Externo UP e DOWN” com `externoModo=UP`, reinicie o mock em falha e
repita com `externoModo=DOWN`. Se parar completamente o mock, execute só a requisição via API:
a chamada direta ao mock terá erro de conexão, não uma resposta HTTP 503.

Em `EnderecoService`, `GET /eventos/{id}/endereco` aplica `@Retry(maxRetries = 2, delay = 200ms, jitter = 50ms)`,
`@ExponentialBackoff(factor = 2, maxDelay = 1000ms)`, `@Timeout(1000ms)` e `@Fallback`. Com o mock
indisponível, o fallback responde HTTP 200 com o `cep` original e valores genéricos (`"logradouro"`,
`"bairro"`, `"cidade"`, `"uf"`). Com o mock de volta, a API retorna o endereço real.

## Coleção Postman e demonstração final

Importe `postman/projeto-final.postman_collection.json` (v2.1). As assertions estão nas
requisições e no nível da coleção (contrato comum dos erros `/eventos`). Use as variáveis de
coleção, sem variáveis de ambiente homônimas que as ocultem.

1. Execute “CRUD”: cria evento próprio, salva `eventoId`, lista, consulta, atualiza e comprova o
   PUT por GET. `dataFutura` é renovada antes das requisições.
2. Execute “Regra de negocio e contador”: captura a métrica, inscreve duas vezes, rejeita
   lotação e redução de capacidade, comprova estado preservado e contador aumentado em dois.
   O contador só aparece em `/q/metrics` após a primeira inscrição; antes disso, conta como 0.
   Não execute tráfego de inscrição concorrente durante a comparação.
3. Execute “Erros e validacao”: POST inválido, PUT vazio e id inexistente. O 409 já é coberto
   pela pasta de regra. O contrato inclui mensagens não vazias, timestamp e `campos`.
4. Execute os cenários de “Externo UP e DOWN”, “Profiles” e “Health UP e DOWN” (que inclui
   `GET /ready/up` e `GET /ready/down` para demonstrar readiness `200 UP` e `503 DOWN`).
5. Execute “Limpeza CRUD” por último: DELETE 204 sem corpo e GET 404 após remoção.

## Demonstração ponta a ponta (script Python)

`e2e/demonstracao_e2e.py` usa só a biblioteca padrão do Python 3.10+ e JDK 21. Ele empacota a API
e o mock, sobe ambos com `java -jar` (profile prod) em portas livres e banco H2 em arquivo
temporário. Depois, percorre os casos de uso, imprimindo cada requisição e o resultado:

| Seção | Requisitos | O que comprova |
|-------|------------|----------------|
| Plataforma | 04, 07 | `Profile prod activated`, porta e JDBC por variável de ambiente, OpenAPI |
| Health | 08 | liveness/readiness UP/DOWN e checks registrados |
| CRUD | 01, 02, 04, 12, 13 | POST 201 + `Location`, GET, listagem, PUT persistido |
| Regras | 05, 10, 13 | lotação 409, capacidade abaixo dos inscritos 409, estado preservado, 20 inscrições simultâneas em 5 vagas (5×200, 15×409), contador igual às aceitas |
| Erros | 02, 14–16 | 400 com cada campo, PUT `{}`, JSON malformado, tipo errado, 415, 405, 404 de evento e de rota, sempre no contrato `codigo`/`mensagem`/`timestamp`/`campos` |
| Externo UP | 06 | mock 200/404, endereço real via API, CEP desconhecido sem 5xx |
| Externo DOWN | 09 | mock parado e `MOCK_FALHAR=true`: fallback controlado; recuperação ao religar |
| Reinício | 07, 08, 13 | dados sobrevivem ao restart em prod |
| Limpeza | 01, 02, 12 | DELETE 204 sem corpo, GET/DELETE seguintes 404, remoção dos dados criados |

```powershell
python e2e\demonstracao_e2e.py                 # build + demonstração completa
python e2e\demonstracao_e2e.py --sem-build     # reaproveita os jars
python e2e\demonstracao_e2e.py --mvn mvn --verboso
python e2e\demonstracao_e2e.py --usar-servicos-existentes --api-url http://localhost:8080
```

O relatório fica em `target/e2e/relatorio.md` e os logs em `target/e2e/*.log`.

## Verificação local

Comandos para reproduzir, sem Docker:

```powershell
.\mvnw.cmd spotless:check test
.\mvnw.cmd -f .\mock-enderecos\pom.xml test
```

Se o wrapper não conseguir baixar Maven por restrição de proxy, um Maven já instalado pode
executar os mesmos goals (`mvn spotless:check test`). Use JDK 21 também em `JAVA_HOME`;
o `java` encontrado primeiro no `PATH` pode ser de outra versão.
