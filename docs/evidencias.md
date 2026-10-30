# Evidências dos requisitos

## Escopo e leitura da matriz

Esta consolidação descreve **o código presente nesta branch**, não o resultado futuro dos merges.
As partes de Felipe (CRUD/service) e Romulo (integração/observabilidade) ainda não estão aqui.
`EventoResource`, `EventoService`, `EnderecoClient`, `EnderecoService`, checks próprios e
`EventoMetricas` são contratos descritos em [divisao-do-trabalho.md](divisao-do-trabalho.md),
**não arquivos existentes**. Não foram criadas versões paralelas dessas implementações.

Os caminhos Java abaixo são relativos a `src/main/java/br/com/equipe/eventos/`, salvo indicação.
“Implementado” significa código e evidência local; “Parcial” identifica base existente;
“Pendente” identifica dependência real. A presença de uma requisição Postman não comprova sua execução.

| Req | Requisito | Código presente / dependência | Evidência reproduzível | Estado nesta branch |
|-----|-----------|------------------------------|------------------------|---------------------|
| 01 | Recursos e métodos HTTP | Depende de `EventoResource` (Felipe) | Postman: CRUD, inscrição e limpeza | Pendente de integração |
| 02 | Status e contratos | Mappers de erros presentes; sucesso depende do CRUD | `ErroRespostaTest`: 400/404/409; Postman: 201 + Location, 200, 204 sem corpo | Parcial |
| 03 | Lombok consciente | `evento/repository/Evento.java`, `platform/erro/ErroResposta.java`, processador em `pom.xml` | `EventoRepositoryTest`, builders usados pelos mappers; explicação abaixo | Implementado |
| 04 | Quarkus/JAX-RS | Extensões REST/Jackson e mappers na API; recurso do mock | `mock-enderecos/.../EnderecoResourceTest`; API de eventos depende de Felipe | Parcial |
| 05 | CDI e regra de negócio | Repository CDI e exceções presentes; falta `EventoService` | Postman: duas inscrições, lotação e redução de capacidade; teste atual verifica mapper, não a regra | Pendente de integração |
| 06 | API externa | Mock HTTP presente; client/resource da API dependem de Romulo | `EnderecoResourceTest`; Postman: externo UP/DOWN | Parcial |
| 07 | Configuração e profiles | H2 `%dev`/`%prod` em `application.properties`; mock usa `@ConfigProperty` | Roteiro de profiles abaixo; falta propriedade própria da API de Romulo | Parcial |
| 08 | Liveness e readiness | Extensões Health e datasource; faltam checks próprios | Postman: health agregado; condição de manutenção depende de Romulo | Parcial |
| 09 | Tolerância a falhas | Extensão instalada; faltam retry/timeout/fallback | Postman: externo UP/DOWN, com pré-condições abaixo | Pendente de integração |
| 10 | Métrica de negócio | Registry Prometheus instalado; falta contador e ligação ao service | Postman: captura antes/depois, diferença de 2 e rejeições sem incremento | Pendente de integração |
| 11 | Entidade JPA | `evento/repository/Evento.java`, H2 e `import.sql` | `EventoRepositoryTest.persisteEConsultaEvento` | Implementado |
| 12 | CRUD Panache Repository | `EventoRepository` herda Panache; falta service/recurso CRUD | Teste de persistência presente; roteiro Postman CRUD preparado | Parcial |
| 13 | Transações e atualização | Teste usa `@TestTransaction`; escritas HTTP dependem de Felipe | Postman: PUT → GET, conflitos → GET e DELETE → GET | Pendente de integração |
| 14 | Bean Validation | `evento/boundary/EventoRequest.java` | `ErroRespostaTest`: POST/PUT inválidos, ausências, limites e entrada válida | Implementado; integrar `@Valid` no CRUD |
| 15 | ExceptionMappers | Três mappers `@Provider` em `platform/erro/` e handlers técnicos | `ErroRespostaTest`: 400, 404, 409, JSON inválido, rota, método, mídia e 500 | Implementado |
| 16 | Corpo padronizado | `platform/erro/ErroResposta.java` e mappers | RestAssured verifica status, chaves exatas, código, mensagem, timestamp ISO e campos | Implementado; repetir no CRUD após integração |

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

O resource definitivo deve usar `@NotNull @Valid EventoRequest` em POST e PUT, antes de chamar
o service. **Ainda não há `/eventos` nesta branch**: `ErroResource` vive exclusivamente em
`src/test/java`, em `/teste-erros`, para exercitar validação automática e descoberta real dos
mappers via HTTP. Ele não será incluído no artefato de produção. Os testes 404/409 disparam as
exceções de domínio diretamente: comprovam os mappers, não consultas nem regras do futuro service.

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

| Chave existente | dev/test | prod | Variável de ambiente |
|----------------|----------|------|----------------------|
| `quarkus.datasource.jdbc.url` | `jdbc:h2:mem:eventos;DB_CLOSE_DELAY=-1` | `jdbc:h2:file:./data/eventos;DB_CLOSE_DELAY=-1` | `QUARKUS_DATASOURCE_JDBC_URL` |
| `quarkus.hibernate-orm.schema-management.strategy` | `drop-and-create` | `update` | `QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY` |
| `quarkus.http.port` | 8080 | 8080 | `QUARKUS_HTTP_PORT` |
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

Confirme `Profile dev activated`/`Profile prod activated` no log. As requisições Postman
“Instancia dev/prod acessivel” verificam somente acessibilidade, não inspecionam o profile.
Após integrar o CRUD, a comprovação de persistência em prod é: criar evento, anotar id,
reiniciar o mesmo jar com o mesmo arquivo H2 e consultar o id. Em dev o banco é recriado.
Remova variáveis no terminal ao terminar (`Remove-Item Env:\QUARKUS_HTTP_PORT`, etc.).
A propriedade própria da API lida com `@ConfigProperty` e valores `%dev`/`%prod` ainda depende
da parte de Romulo; o health acessível sozinho não satisfaz esse requisito.

## 08 — Health e condições DOWN

As extensões fornecem endpoints agregados `/q/health/live` e `/q/health/ready`; a integração
Agroal/H2 fornece readiness do datasource. Não há check próprio em `platform/health/`.
Sem checks liveness próprios, `UP` agregado não comprova uma condição de negócio.
Readiness do datasource pode ficar `DOWN` quando a conexão/verificação do banco falha.

Após integrar Romulo, documentar o nome exato e as condições reais dos checks. O contrato
planejado é `eventos.manutencao=true` (`EVENTOS_MANUTENCAO=true`) derrubar readiness para
503/`DOWN`, mantendo liveness 200/`UP`. **Essa chave não existe ainda nesta branch**.
Na coleção, ajuste `readinessEsperada` para `DOWN` apenas ao executar essa demonstração;
restaure a configuração e a variável para `UP` depois.

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

`GET /eventos/{id}/endereco`, retry/timeout/fallback e os campos de sucesso/fallback dependem
da parte de Romulo. O roteiro assume sucesso com campos do endereço e fallback HTTP 200 com
`cep`/`disponivel=false`, conforme o exemplo do contrato de divisão. Confirmar esse formato
na integração antes de registrar a demonstração como aprovada; não há teste de fallback executado aqui.

## Coleção Postman e demonstração final

Importe `postman/projeto-final.postman_collection.json` (v2.1). As assertions estão nas
requisições e no nível da coleção (contrato comum dos erros `/eventos`). Use as variáveis de
coleção, sem variáveis de ambiente homônimas que as ocultem.

1. Após integrar Felipe/Romulo, execute “CRUD”: cria evento próprio, salva `eventoId`, lista,
   consulta, atualiza e comprova o PUT por GET. `dataFutura` é renovada antes das requisições.
2. Execute “Regra de negocio e contador”: captura a métrica, inscreve duas vezes, rejeita
   lotação e redução de capacidade, comprova estado preservado e contador aumentado em dois.
   Não execute tráfego de inscrição concorrente durante a comparação.
3. Execute “Erros e validacao”: POST inválido, PUT vazio e id inexistente. O 409 já é coberto
   pela pasta de regra. O contrato inclui mensagens não vazias, timestamp e `campos`.
4. Execute manualmente os dois estados de “Externo UP e DOWN”, “Profiles” e “Health”, com
   suas pré-condições. Reiniciar dev apaga o banco: recrie o evento se isso acontecer.
5. Execute “Limpeza CRUD” por último: DELETE 204 sem corpo e GET 404 após remoção.

A coleção é um roteiro com assertions, não uma simulação de resultados. A execução end-to-end
permanece pendente das implementações ausentes; não há relatório Postman aprovado nesta branch.

## Verificação local

Comandos para reproduzir, sem Docker:

```powershell
.\mvnw.cmd spotless:check test
.\mvnw.cmd -f .\mock-enderecos\pom.xml test
```

Se o wrapper não conseguir baixar Maven por restrição de proxy, um Maven já instalado pode
executar os mesmos goals (`mvn spotless:check test`). Use JDK 21 também em `JAVA_HOME`;
o `java` encontrado primeiro no `PATH` pode ser de outra versão.

Verificação desta entrega: `mvn spotless:check test` aprovado (13 testes da API: 12 de erros
e um de persistência); `mvn -f mock-enderecos/pom.xml test` aprovado (dois testes);
`mvn -DskipTests package` aprovado. O erro registrado no cenário de 500 é intencional e
confirma que a falha não é silenciada. A coleção contém 26 requisições e 28 scripts com
sintaxe JavaScript verificada, inclusive o caso de JSON propositalmente malformado.
Isso não equivale a uma execução Postman end-to-end.
