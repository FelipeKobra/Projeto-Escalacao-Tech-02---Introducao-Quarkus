# Eventos API — Projeto final Introdução ao Quarkus

Projeto de API REST para **gestão de eventos com inscrições limitadas às vagas**. Cada evento
tem nome, CEP, data/hora e capacidade. A API oferece CRUD de eventos, controle de inscrições com
recusa em evento lotado, consulta de endereço em serviço HTTP externo (`mock-enderecos`) com
tolerância a falhas (retry, timeout, backoff exponencial e fallback), validação de entrada,
respostas de erro padronizadas, health checks (liveness/readiness) e métricas de negócio.

Veja a [matriz dos requisitos 01 a 16](docs/evidencias.md) para o detalhamento técnico e roteiros
de verificação de cada requisito.

## Equipe

| Integrante | Parte |
|------------|-------|
| Felipe | CRUD, regra de inscrição e transações |
| Romulo | Integração externa, resiliência, profiles, health e métricas |
| Wellington | Validação, tratamento de erros, Lombok, Postman e evidências |

Contratos entre as partes: [docs/divisao-do-trabalho.md](docs/divisao-do-trabalho.md).

## Stack e organização

Java 21, Quarkus 3.40.1.1 (LTS), Maven wrapper (`./mvnw`), Hibernate ORM/Panache e H2 sem Docker.
O `pom.xml` inclui Quarkus REST + Jackson, REST Client, Hibernate Validator, SmallRye Health,
SmallRye Fault Tolerance, Micrometer (Prometheus), SmallRye OpenAPI e Lombok.

A organização segue Package by Feature + Boundary/Domain/Repository:

| Local | Conteúdo |
|-------|----------|
| `src/main/java/br/com/equipe/eventos/evento/boundary` | `EventoResource`, `EventoEnderecoResource`, `ReadinessResource` e records DTO (`EventoRequest`, `EventoResponse`, `EventoEnderecoResponse`) |
| `src/main/java/br/com/equipe/eventos/evento/domain` | `EventoService`, regras de negócio e exceções de domínio |
| `src/main/java/br/com/equipe/eventos/evento/repository` | Entidade JPA `Evento` e `EventoRepository` (PanacheRepository) |
| `src/main/java/br/com/equipe/eventos/integracao/enderecos` | `EnderecoClient` (REST Client), `EnderecoService` (retry/timeout/fallback) e `EnderecoExterno` |
| `src/main/java/br/com/equipe/eventos/platform/erro` | Contrato `ErroResposta`, três `ExceptionMapper`s de domínio e handlers técnicos |
| `src/main/java/br/com/equipe/eventos/platform/health` | `AplicacaoLivenessCheck`, `ProntidaoReadinessCheck` e `ReadinessService` |
| `src/main/java/br/com/equipe/eventos/platform/metrics` | `EventoMetricas` (`eventos.inscricoes` / `eventos_inscricoes_total`) |
| `mock-enderecos` | Outro app Quarkus que simula o serviço HTTP externo na porta 8081 |
| `src/test/java` | Testes automatizados (`EventoResourceTest`, `EventoServiceTest`, `EventoRepositoryTest`, `ErroRespostaTest`) |

H2 atende ao enunciado sem exigir servidor de banco ou Docker. Em `dev`/`test` é em memória e
recriado a cada execução; em `prod` usa arquivo em `./data/`, preservado entre reinícios. O esquema
usa `drop-and-create` em `dev`/`test` e `update` em `prod`.

## Executar

Pré-requisito: **JDK 21**, com `JAVA_HOME` configurado. No PowerShell, a partir da raiz:

```powershell
# Terminal 1: serviço externo simulado, porta 8081
.\mvnw.cmd -f .\mock-enderecos\pom.xml quarkus:dev

# Terminal 2: API, porta 8080, profile dev
.\mvnw.cmd quarkus:dev
```

No Linux/macOS, substitua `.\mvnw.cmd` por `./mvnw` e use separadores `/`.
O wrapper baixa Maven no primeiro uso; ambientes corporativos podem exigir configuração
de proxy/repositório. Se Maven já estiver instalado, `mvn` aceita os mesmos goals.

Swagger UI: http://localhost:8080/q/swagger-ui · Dev UI: http://localhost:8080/q/dev-ui.
O recurso `/teste-erros` existe **somente nos testes**, nunca no jar de produção.

### Produção e profiles

```powershell
.\mvnw.cmd package
# Em outro terminal, para coexistir com dev:
$env:QUARKUS_HTTP_PORT = "8082"
$env:QUARKUS_DATASOURCE_JDBC_URL = "jdbc:h2:file:./data/demonstracao;DB_CLOSE_DELAY=-1"
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\quarkus-app\quarkus-run.jar
```

O log identifica o profile: `quarkus:dev` usa `dev`, os testes usam `test`, e o jar usa `prod`.
Depois de encerrar, remova as variáveis do terminal com `Remove-Item Env:\QUARKUS_HTTP_PORT`
e `Remove-Item Env:\QUARKUS_DATASOURCE_JDBC_URL` para não afetar outra execução.

| Chave | dev/test | prod | Variável de ambiente |
|-------|----------|------|----------------------|
| `quarkus.datasource.jdbc.url` | `jdbc:h2:mem:eventos;DB_CLOSE_DELAY=-1` | `jdbc:h2:file:./data/eventos;DB_CLOSE_DELAY=-1` | `QUARKUS_DATASOURCE_JDBC_URL` |
| `quarkus.hibernate-orm.schema-management.strategy` | `drop-and-create` | `update` | `QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY` |
| `quarkus.http.port` | 8080 (testes: aleatória) | 8080 | `QUARKUS_HTTP_PORT` |
| `quarkus.rest-client.enderecos-api.url` | `http://localhost:8081` | `http://localhost:8081` | `QUARKUS_REST_CLIENT_ENDERECOS_API_URL` |
| `eventos.app` | `API Eventos - DEV` | `API Eventos` (`%prd`) | `EVENTOS_APP` |
| `mock.falhar` (mock separado) | `false` | `false` | `MOCK_FALHAR` |

### Simular indisponibilidade externa

Encerre o mock e reinicie no mesmo terminal:

```powershell
$env:MOCK_FALHAR = "true"
.\mvnw.cmd -f .\mock-enderecos\pom.xml quarkus:dev
# Após encerrar o mock:
Remove-Item Env:\MOCK_FALHAR
```

O mock retorna 503 nesse modo; sem a variável, CEP conhecido retorna 200 e desconhecido 404.
Também é possível parar o mock completamente para simular falha de conexão. Com o mock indisponível,
`GET /eventos/{id}/endereco` aciona `@Retry`, `@Timeout` e `@Fallback` no `EnderecoService`,
respondendo 200 com o `cep` do evento e valores genéricos em vez de erro 500.

## Endpoints e contratos

| Método | URI | Resposta |
|--------|-----|----------|
| GET | `/eventos` | 200, lista de eventos |
| GET | `/eventos/{id}` | 200 ou 404 (`EVENTO_NAO_ENCONTRADO`) |
| POST | `/eventos` | 201 + `Location`, ou 400 (`REQUISICAO_INVALIDA`) |
| PUT | `/eventos/{id}` | 200, 400, 404 ou 409 (`CAPACIDADE_ABAIXO_DOS_INSCRITOS`) |
| DELETE | `/eventos/{id}` | 204 sem corpo, ou 404 (`EVENTO_NAO_ENCONTRADO`) |
| POST | `/eventos/{id}/inscricoes` | 200, 404 ou 409 se lotado (`EVENTO_LOTADO`) |
| GET | `/eventos/{id}/endereco` | 200 com endereço externo ou fallback (404 se evento não existir) |
| GET | `/ready/up` e `/ready/down` | 200, alternam o estado de readiness para demonstração |
| GET | `/q/health/live` e `/q/health/ready` | Liveness e readiness (SmallRye Health) |
| GET | `/q/metrics` | Métricas Prometheus (`eventos_inscricoes_total`) |

Exemplo de entrada (use uma data futura ao executar):

```json
{
  "nome": "Workshop de Quarkus",
  "descricao": "Introdução prática",
  "cep": "01310100",
  "dataHora": "2099-12-10T19:00:00",
  "capacidade": 30
}
```

Nome é obrigatório (até 120 caracteres), descrição opcional (até 500), CEP deve ter exatamente
oito dígitos, data/hora deve ser futura e capacidade positiva. Em `EventoResource`, POST e PUT
recebem `@Valid EventoRequest`.

Os erros REST têm `codigo`, `mensagem`, `timestamp` ISO com offset e `campos` (lista vazia
fora da validação). Validação retorna 400, evento ausente 404 e regra de negócio 409,
preservando seu código. JSON inválido e erros HTTP também seguem o contrato; erros inesperados
retornam 500 sem expor detalhes internos e são registrados no servidor.

## Health, métricas e demonstrações

- `/q/health/live` expõe `AplicacaoLivenessCheck` (nome configurado por `eventos.app`).
- `/q/health/ready` expõe `ProntidaoReadinessCheck` além do check do datasource H2; os endpoints
  auxiliares `GET /ready/up` e `GET /ready/down` controlam o estado de prontidão em tempo de execução.
- `/q/metrics` expõe o contador `eventos_inscricoes_total` (`EventoMetricas`), incrementado a cada
  inscrição concluída com sucesso em `EventoService.inscrever`.

Importe a [coleção Postman](postman/projeto-final.postman_collection.json) e execute as pastas em
ordem: CRUD → regra/métricas → erros → externo/health/profiles → limpeza. A coleção cria um evento
próprio, guarda o id e gera datas futuras automaticamente.

### Demonstração ponta a ponta automatizada

```powershell
python e2e\demonstracao_e2e.py
```

O script ([e2e/demonstracao_e2e.py](e2e/demonstracao_e2e.py)) só usa a biblioteca padrão do
Python e exige JDK 21. Ele empacota e sobe API e mock em profile prod, depois percorre CRUD,
regras, concorrência, métrica, erros, externo UP/DOWN com fallback, health, persistência após
reinício e limpeza, gerando `target/e2e/relatorio.md`. Opções e resultados:
[evidências](docs/evidencias.md#demonstração-ponta-a-ponta-script-python).

## Testes e formatação

```powershell
.\mvnw.cmd test
.\mvnw.cmd -f .\mock-enderecos\pom.xml test
.\mvnw.cmd spotless:check
.\mvnw.cmd spotless:apply
```

RestAssured e JUnit cobrem CRUD completo, regras de negócio (`EventoResourceTest`, `EventoServiceTest`),
validação POST/PUT, ausências e limites, erros 400/404/409, JSON inválido, rota inexistente,
405/415 e 500 padronizado (`ErroRespostaTest`), persistência (`EventoRepositoryTest`) e o mock
externo (`EnderecoResourceTest`).

## Evidências dos requisitos

Detalhes do Lombok, justificativa para não usar `@Data` na entidade, matriz completa dos 16
requisitos e roteiro de verificação: [docs/evidencias.md](docs/evidencias.md).
