# Roteiro da apresentação — Eventos API

Ordem: **Felipe → Romulo → Gean** (~5 min cada). Cada um mostra o código da sua parte e roda os
comandos `curl.exe` ao vivo. Os comandos são para **PowerShell**, a partir da raiz do projeto.
Use `curl.exe`, não `curl`: no PowerShell `curl` é um alias de `Invoke-WebRequest`.

Os corpos JSON ficam em [`demo/`](../demo) e vão com `-d "@demo/arquivo.json"`. Isso evita o
problema de aspas do PowerShell com JSON inline.

| Opção | Para que serve |
|-------|----------------|
| `-s` | Esconde a barra de progresso |
| `-i` | Mostra status e cabeçalhos (`201`, `Location`, `409`...) |
| `-X` | Método HTTP (`POST`, `PUT`, `DELETE`) |
| `-H` | Cabeçalho (`Content-Type: application/json`) |

## Preparação (antes de o professor chegar)

```powershell
# Terminal 1 — serviço externo simulado (porta 8081)
.\mvnw.cmd -f .\mock-enderecos\pom.xml quarkus:dev

# Terminal 2 — API em profile dev (porta 8080)
.\mvnw.cmd quarkus:dev

# Terminal 3 — onde rodamos os comandos
$API = "http://localhost:8080"
curl.exe -s "$API/q/health/live"          # tem que responder "status": "UP"
```

> **Porta 8080 ocupada?** (no PC do Felipe o container `traefik` do Docker usa a 8080 e a 8082)
> Suba a API com `.\mvnw.cmd quarkus:dev "-Dquarkus.http.port=8090"` e use
> `$API = "http://localhost:8090"`. Todos os comandos abaixo usam `$API`.

O banco H2 em memória recarrega `import.sql` a cada start:
**evento 1** (Workshop de Quarkus, 30 vagas livres) e **evento 2** (Meetup Java, lotado 2/2).
Se precisar recomeçar a demonstração do zero, reinicie a API (Terminal 2).

---

## 1. Felipe — CRUD, regra de negócio e transações

**Requisitos:** 01, 02, 04, 05, 11, 12, 13

**Código para mostrar:**
- `evento/boundary/EventoResource.java`: `@Path`, `@GET/@POST/@PUT/@DELETE`, `@Produces/@Consumes`,
  `201 + Location` no POST, `void` no DELETE (204). A inscrição é um subrecurso
  (`/eventos/{id}/inscricoes`), sem verbo na URI.
- `evento/domain/EventoService.java`: `@ApplicationScoped` injetado por construtor (CDI),
  `@Transactional` nas escritas, regras `EVENTO_LOTADO` e `CAPACIDADE_ABAIXO_DOS_INSCRITOS`,
  lock pessimista na inscrição.
- `evento/repository/Evento.java` (`@Entity`) e `EventoRepository.java`: `PanacheRepository`
  (`listAll`, `findByIdOptional`, `persist`, `delete`).

**Comandos:**

```powershell
# F1) Listar eventos — 200
curl.exe -s "$API/eventos"

# F2) Buscar um evento — 200
curl.exe -s "$API/eventos/1"

# F3) Criar evento — 201 Created + header Location (/eventos/3)
curl.exe -s -i -X POST "$API/eventos" -H "Content-Type: application/json" -d "@demo/evento-novo.json"

# F4) Atualizar (PUT) — 200 com os dados novos
curl.exe -s -X PUT "$API/eventos/3" -H "Content-Type: application/json" -d "@demo/evento-atualizado.json"

# F5) Consultar de novo: a alteração foi persistida (Req 13 — PUT → GET)
curl.exe -s "$API/eventos/3"

# F6) Inscrição com vaga — 200, inscritos sobe para 1 e vagasDisponiveis cai para 29
curl.exe -s -X POST "$API/eventos/1/inscricoes"

# F7) Regra de negócio: inscrição em evento lotado — 409 EVENTO_LOTADO
curl.exe -s -i -X POST "$API/eventos/2/inscricoes"

# F8) Regra de negócio: capacidade abaixo dos inscritos — 409 CAPACIDADE_ABAIXO_DOS_INSCRITOS
curl.exe -s -i -X PUT "$API/eventos/2" -H "Content-Type: application/json" -d "@demo/evento-capacidade-baixa.json"

# F9) Remover — 204 No Content (sem corpo)
curl.exe -s -i -X DELETE "$API/eventos/3"

# F10) Depois de remover — 404
curl.exe -s -i "$API/eventos/3"
```

**Pontos para falar:** o resource só traduz HTTP ↔ DTO (records `EventoRequest`/`EventoResponse`)
e delega ao service. A regra fica no service, não no repositório. No PUT não há `persist`: a
entidade é gerenciada, e o `@Transactional` grava a alteração no commit.

---

## 2. Romulo — Integração externa, resiliência, profiles, health e métricas

**Requisitos:** 06, 07, 08, 09, 10

**Código para mostrar:**
- `integracao/enderecos/EnderecoClient.java`: `@RegisterRestClient(configKey = "enderecos-api")`.
  A URL vem de `quarkus.rest-client.enderecos-api.url` em `application.properties`.
- `integracao/enderecos/EnderecoService.java`: `@Retry`, `@ExponentialBackoff`, `@Timeout`, `@Fallback`.
- `platform/health/`: `@Liveness` e `@Readiness` (+ `ReadinessService`).
- `platform/metrics/EventoMetricas.java`: `Counter` do Micrometer chamado em `EventoService.inscrever`.
- `application.properties`: `eventos.app` (base) × `%dev.eventos.app`, `eventos.manutencao`,
  H2 em memória × `%prod` em arquivo.

**Comandos — API externa e tolerância a falhas (06, 09):**

```powershell
# R1) Serviço externo direto (mock na 8081)
curl.exe -s "http://localhost:8081/enderecos/01310100"

# R2) A API consulta o externo via REST Client — endereço real (Avenida Paulista)
curl.exe -s "$API/eventos/1/endereco"

# R3) Agora PARE o mock (Ctrl+C no Terminal 1) e repita:
curl.exe -s "http://localhost:8081/enderecos/01310100"   # falha de conexão
curl.exe -s -i "$API/eventos/1/endereco"                 # 200 com fallback, não 500

# (alternativa) subir o mock respondendo 503 em tudo, no Terminal 1:
#   $env:MOCK_FALHAR = "true"; .\mvnw.cmd -f .\mock-enderecos\pom.xml quarkus:dev
#   ...depois: Remove-Item Env:\MOCK_FALHAR  e suba o mock normal de novo
```

O fallback devolve o CEP do evento com valores genéricos (`"logradouro": "logradouro"`...).
Antes disso, o `@Retry` tenta 2 vezes com backoff exponencial, e o `@Timeout` corta em 1 s.

**Comandos — liveness e readiness (08):**

```powershell
# R4) Liveness — UP (o nome "API Eventos - DEV" vem da config do profile dev)
curl.exe -s "$API/q/health/live"

# R5) Readiness — UP (check próprio + check do banco H2)
curl.exe -s -i "$API/q/health/ready"

# R6) Tirar a API de prontidão sem derrubar o processo
curl.exe -s "$API/ready/down"
curl.exe -s -i "$API/q/health/ready"     # 503 DOWN
curl.exe -s "$API/q/health/live"         # liveness continua UP

# R7) Voltar
curl.exe -s "$API/ready/up"
curl.exe -s -i "$API/q/health/ready"     # 200 UP
```

**Comandos — métrica de negócio (10):**

```powershell
# R8) Valor atual do contador (o Felipe já fez 1 inscrição)
curl.exe -s "$API/q/metrics" | Select-String "eventos_inscricoes_total"

# R9) Uma inscrição nova e o contador sobe
curl.exe -s -X POST "$API/eventos/1/inscricoes"
curl.exe -s "$API/q/metrics" | Select-String "eventos_inscricoes_total"
```

**Comandos — profiles e variável de ambiente (07)**, num 4º terminal com o jar em prod:

```powershell
.\mvnw.cmd package -DskipTests
$env:QUARKUS_HTTP_PORT = "8083"
$env:EVENTOS_MANUTENCAO = "true"
java -jar .\target\quarkus-app\quarkus-run.jar      # log: "Profile prod activated"

# no Terminal 3:
curl.exe -s "http://localhost:8083/q/health/live"      # nome "API Eventos" (sem "- DEV")
curl.exe -s -i "http://localhost:8083/q/health/ready"  # 503: EVENTOS_MANUTENCAO=true sobrescreveu a config

# ao terminar (Ctrl+C no 4º terminal e depois):
Remove-Item Env:\QUARKUS_HTTP_PORT, Env:\EVENTOS_MANUTENCAO
```

**Pontos para falar:** liveness = "o processo está vivo?" (DOWN → reiniciar); readiness = "pode
receber tráfego?" (DOWN → tirar do balanceador, sem reiniciar). Qualquer chave pode ser sobrescrita
por variável de ambiente (`eventos.manutencao` → `EVENTOS_MANUTENCAO`). Em prod o H2 é arquivo
(`./data/`), então os dados sobrevivem ao restart; o `import.sql` só carrega em dev/test.

---

## 3. Gean — Validação, erros padronizados, Lombok e entregáveis

**Requisitos:** 03, 14, 15, 16 + coleção Postman, script e2e e evidências

**Código para mostrar:**
- `evento/boundary/EventoRequest.java`: `@NotBlank`, `@Size`, `@Pattern` (CEP com 8 dígitos),
  `@Future`, `@Positive`, `@NotNull`. O `@Valid` fica no `EventoResource`.
- `platform/erro/ErroResposta.java`: record com `@Builder` do Lombok (`codigo`, `mensagem`,
  `timestamp`, `campos`).
- `platform/erro/`: `ValidacaoMapper` (400), `EventoNaoEncontradoMapper` (404),
  `RegraDeNegocioMapper` (409) e `ErrosTecnicosMapper` (JSON inválido, 404/405/415 do framework,
  500). Não há try/catch nos endpoints.
- `evento/repository/Evento.java`: `@Getter/@Setter/@NoArgsConstructor` e por que **não** `@Data`
  numa entidade JPA (equals/hashCode/toString com id mutável e lazy loading).

**Comandos:**

```powershell
# G1) Bean Validation — 400 com a lista de campos inválidos
curl.exe -s -i -X POST "$API/eventos" -H "Content-Type: application/json" -d "@demo/evento-invalido.json"

# G2) Evento inexistente — 404 EVENTO_NAO_ENCONTRADO, mesmo formato
curl.exe -s -i "$API/eventos/999"

# G3) Regra de negócio — 409 com o código da exceção, mesmo formato
curl.exe -s -i -X POST "$API/eventos/2/inscricoes"

# G4) JSON malformado — 400 REQUISICAO_INVALIDA, mesmo formato
curl.exe -s -i -X POST "$API/eventos" -H "Content-Type: application/json" -d "@demo/json-malformado.json"

# G5) Rota que não existe — 404 HTTP_404, mesmo formato
curl.exe -s -i "$API/rota-que-nao-existe"

# G6) Content-Type errado — 415 HTTP_415, mesmo formato
curl.exe -s -i -X POST "$API/eventos" -H "Content-Type: text/plain" -d "texto"
```

Todos os erros devolvem as mesmas quatro chaves: `codigo`, `mensagem`, `timestamp` e `campos`.

**Entregáveis para mostrar no fim:**

```powershell
# Testes automatizados (JUnit + RestAssured) — 33 testes
.\mvnw.cmd test

# Demonstração ponta a ponta (sobe API + mock sozinho em portas livres) — 41 passos OK
python e2e\demonstracao_e2e.py --sem-build     # sem --sem-build ele recompila antes
```

Coleção Postman: `postman/projeto-final.postman_collection.json`. Matriz dos 16 requisitos:
[docs/evidencias.md](evidencias.md).

---

## Cola rápida: requisito → quem mostra

| Req | Tema | Quem | Comando |
|-----|------|------|---------|
| 01, 04 | Recursos, métodos, JAX-RS | Felipe | F1–F10 |
| 02 | Status 200/201/204/400/404/409 | Felipe + Gean | F3, F9, F10, G1–G3 |
| 03 | Lombok consciente | Gean | código `Evento` e `ErroResposta` |
| 05 | CDI + regra de negócio | Felipe | F7, F8 |
| 06 | API externa | Romulo | R1, R2 |
| 07 | Config e profiles | Romulo | jar prod na 8083 |
| 08 | Liveness/readiness | Romulo | R4–R7 |
| 09 | Tolerância a falhas | Romulo | R3 (mock parado) |
| 10 | Métrica de negócio | Romulo | R8, R9 |
| 11 | Entidade JPA | Felipe | código `Evento` |
| 12 | CRUD Panache | Felipe | F1–F10 |
| 13 | Transação, PUT → GET | Felipe | F4, F5 |
| 14 | Bean Validation | Gean | G1 |
| 15, 16 | ExceptionMapper + erro padronizado | Gean | G1–G6 |
