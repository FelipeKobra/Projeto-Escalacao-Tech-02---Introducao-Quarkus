# Divisão do trabalho

Os 16 requisitos do enunciado (`trabalho-final.pdf`) estão divididos em três partes, uma por
integrante, cada uma com sua branch e seu PR. A **base** (já na `main`) traz o que as três partes
compartilham, para ninguém ficar esperando o outro para começar.

## O que já está pronto na base (`main`)

| Item | Onde |
|------|------|
| Projeto gerado em code.quarkus.io (Quarkus 3.40.1.1, Java 21) com todas as extensões | `pom.xml` |
| Lombok + Spotless (Google Java Format) configurados | `pom.xml` |
| H2 em memória (dev/test) e em arquivo (prod) | `src/main/resources/application.properties` |
| Entidade `Evento` com Lombok (**Req 11**, base do **Req 03**) | `evento/repository/Evento.java` |
| `EventoRepository` (PanacheRepository) | `evento/repository/EventoRepository.java` |
| DTOs `EventoRequest` / `EventoResponse` (records) | `evento/boundary/` |
| Exceções de domínio `EventoNaoEncontradoException` e `RegraDeNegocioException` | `evento/domain/` |
| Dados de exemplo (evento 1 com vagas, evento 2 lotado) | `src/main/resources/import.sql` |
| Serviço externo simulado (porta 8081, CEPs 01310100, 20040002, 30130010, 70040010) | `mock-enderecos/` |
| Teste de persistência da entidade | `src/test/.../EventoRepositoryTest.java` |

Cada branch já tem um commit inicial com o esqueleto das classes da parte (métodos com `TODO`) e
um PR em rascunho com o checklist abaixo.

## Felipe — CRUD, regra de negócio e transações

**Branch:** `feat/felipe-crud-eventos` · **Requisitos:** 01, 02, 04, 05, 12, 13

- [ ] `EventoService` (`evento/domain/`, `@ApplicationScoped`) com listar, buscar, criar, atualizar
      e remover, usando o `EventoRepository`; escritas com `@Transactional` (Req 05, 12, 13).
- [ ] Regra de negócio no service, além de repassar ao repositório (Req 05):
  - inscrição em evento lotado lança `RegraDeNegocioException("EVENTO_LOTADO", ...)`;
  - `PUT` não pode deixar a capacidade abaixo do número de inscritos
    (`RegraDeNegocioException("CAPACIDADE_ABAIXO_DOS_INSCRITOS", ...)`).
- [ ] Id inexistente lança `EventoNaoEncontradoException` (o Wellington mapeia para 404).
- [ ] `EventoResource` (`evento/boundary/`) com `@Path("/eventos")` e `@Produces`/`@Consumes` JSON (Req 04):
  - `GET /eventos` → 200, `GET /eventos/{id}` → 200;
  - `POST /eventos` → **201 + header `Location`**;
  - `PUT /eventos/{id}` → 200, `DELETE /eventos/{id}` → **204**;
  - `POST /eventos/{id}/inscricoes` → 200 com o evento atualizado (sem verbo na URI, Req 01).
- [ ] `@Valid` no parâmetro `EventoRequest` do `POST` e do `PUT` (as restrições em si são do Wellington).
- [ ] Chamar `EventoMetricas.registrarInscricao()` do Romulo quando a inscrição der certo (Req 10).
- [ ] Testes `@QuarkusTest` com RestAssured: CRUD completo, sequência PUT → GET mostrando o dado
      alterado (Req 13) e inscrição recusada em evento lotado.

## Romulo — Integração, resiliência, configuração e observabilidade

**Branch:** `feat/romulo-integracao-observabilidade` · **Requisitos:** 06, 07, 08, 09, 10

- [ ] `EnderecoClient` em `integracao/enderecos/` com `@RegisterRestClient(configKey = "enderecos-api")`
      e o record `EnderecoExterno` (cep, logradouro, bairro, cidade, uf) (Req 06).
- [ ] URL em `application.properties`: `quarkus.rest-client.enderecos-api.url=http://localhost:8081`,
      sobrescrevível por `QUARKUS_REST_CLIENT_ENDERECOS_API_URL`.
- [ ] `EnderecoService` com `@Retry` + `@Fallback` (e `@Timeout`): mock fora do ar → resposta
      controlada (ex.: `{"cep": "...", "disponivel": false}`) em vez de 500 (Req 09).
- [ ] `EventoEnderecoResource` em `evento/boundary/` com `GET /eventos/{id}/endereco`. É uma classe
      separada do `EventoResource` do Felipe para não dar conflito no merge.
- [ ] Configuração própria lida com `@ConfigProperty` ou `@ConfigMapping`, com valores diferentes
      em `%dev` e `%prod` e uma variável de ambiente sobrescrevendo (Req 07). Documentar no README
      (seção "Configurações e profiles").
- [ ] `platform/health/`: um `@Liveness` (app viva) e um `@Readiness` com condição controlável,
      ex.: a chave `eventos.manutencao=true` derruba a readiness (Req 08). Explicar em
      `docs/evidencias.md` quando cada uma fica DOWN.
- [ ] `platform/metrics/EventoMetricas` (`@ApplicationScoped`) com um `Counter` do Micrometer
      (`eventos.inscricoes`, aparece como `eventos_inscricoes_total` em `/q/metrics`) e o método
      público `registrarInscricao()` que o Felipe chama (Req 10).
- [ ] Testes: fallback com o serviço externo fora do ar e a métrica variando em `/q/metrics`.

## Wellington — Validação, erros, Lombok e entregáveis

**Branch:** `feat/wellington-validacao-erros-docs` · **Requisitos:** 03, 14, 15, 16 + README, Postman e evidências

- [ ] Restrições de Bean Validation nos componentes do `EventoRequest`, pelo menos duas distintas:
      `@NotBlank`, `@Size(max = 120)`, `@Pattern` (CEP com 8 dígitos), `@Future`, `@Positive` (Req 14).
- [ ] `platform/erro/ErroResposta`: JSON padronizado com `codigo`, `mensagem`, `timestamp` e, na
      validação, a lista de campos inválidos. Usar `@Builder` do Lombok aqui (Req 03 + 16).
- [ ] `ExceptionMapper`s em `platform/erro/`, sem try/catch nos endpoints (Req 15):
  - `EventoNaoEncontradoException` → 404;
  - `RegraDeNegocioException` → 409 (usa o `codigo` da exceção);
  - `ConstraintViolationException` → 400 no formato `ErroResposta`;
  - opcional: `NotFoundException` do JAX-RS (rota inexistente) → 404 no mesmo formato.
- [ ] Testes RestAssured de pelo menos dois erros com o corpo padronizado (400 e 404) (Req 16).
- [ ] Coleção `postman/projeto-final.postman_collection.json` com todas as demonstrações do roteiro
      (CRUD, regra, externo UP/DOWN, profile, health, métricas, erros).
- [ ] `docs/evidencias.md` preenchido (Req 01 a 16) e README revisado no fim.
- [ ] Explicar em `docs/evidencias.md` o código que o Lombok gera em `Evento` e `ErroResposta` e por
      que não usamos `@Data` na entidade (Req 03).

## Pontos de contato entre as partes

| De → Para | Contrato |
|-----------|----------|
| Felipe → Wellington | O service lança `EventoNaoEncontradoException` e `RegraDeNegocioException(codigo, mensagem)` (já na base); o resource usa `@Valid`. |
| Felipe → Romulo | O service chama `EventoMetricas.registrarInscricao()` depois de inscrever. |
| Romulo → Felipe | `GET /eventos/{id}/endereco` busca o evento por `EventoService.buscar(id)`. |

Se um contrato precisar mudar, avise no grupo e comente no PR do colega antes.

## Fluxo de trabalho

```bash
git fetch origin
git checkout feat/<sua-branch>      # a branch já existe, com o PR em rascunho
# ... codar ...
./mvnw spotless:apply               # formata (Google Java Format)
./mvnw test                         # tudo verde antes de subir
git add -A
git commit -m "feat: descrição curta"
git push
```

- Commits em [Conventional Commits](https://www.conventionalcommits.org/pt-br/v1.0.0/):
  `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`.
- Ao terminar, marque o PR como "Ready for review" e peça revisão de pelo menos um colega.
- **Ordem de merge sugerida:** Felipe → Romulo → Wellington. Quem vier depois atualiza a branch com
  `git fetch origin && git merge origin/main`, resolve os conflitos e roda `./mvnw test` de novo.
- Ninguém commita direto na `main`.

## Pronto quando

- [ ] `./mvnw test` e `./mvnw -f mock-enderecos/pom.xml test` verdes.
- [ ] Cada requisito com evidência em `docs/evidencias.md` e requisição na coleção Postman.
- [ ] README com equipe, problema, versões, como rodar, banco, profiles, exemplos e tabela de evidências.
