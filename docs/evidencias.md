# Evidências dos requisitos

Cada linha aponta o código e a requisição (coleção Postman) ou o teste que demonstra o requisito.
Preencher conforme as partes forem integradas. Quem consolida: Wellington.

| # | Requisito | Responsável | Código | Requisição Postman / teste | Status |
|---|-----------|-------------|--------|----------------------------|--------|
| 01 | Recursos e métodos HTTP | Felipe | `evento/boundary/EventoResource.java` | Postman "01-02 CRUD de eventos" (`GET /eventos`, `GET /eventos/{id}`, `POST`, `PUT`, `DELETE`; inscrição como subrecurso `POST /eventos/{id}/inscricoes`, sem verbo na URI); `EventoResourceTest` | ✅ |
| 02 | Códigos de status e contratos | Felipe | `evento/boundary/EventoResource.java` | Postman "01-02 CRUD de eventos": 200 (consulta), 201 + `Location` (Criar evento), 204 (Remover evento); `EventoResourceTest.criarResponde201ComLocationEInscritosZerados`, `removerResponde204`. 400/404 dependem dos mappers do Wellington (testes `@Disabled` em `EventoResourceTest`) | 🟡 |
| 03 | Lombok aplicado conscientemente | Wellington | `evento/repository/Evento.java`, `platform/erro/ErroResposta.java` | | ⬜ |
| 04 | API Quarkus/JAX-RS | Felipe | `evento/boundary/EventoResource.java` | `@Path("/eventos")`, `@GET`/`@POST`/`@PUT`/`@DELETE`, `@Produces`/`@Consumes` JSON; Postman "Listar eventos"; `EventoResourceTest.listarResponde200ComOsEventos` | ✅ |
| 05 | CDI e regra de negócio | Felipe | `evento/domain/EventoService.java` | `EventoService` `@ApplicationScoped` injetado por construtor no resource. Regras: `EVENTO_LOTADO` e `CAPACIDADE_ABAIXO_DOS_INSCRITOS`. Postman "05 Regra de negócio"; `EventoServiceTest.inscreverEmEventoLotadoLancaEventoLotado`, `atualizarCapacidadeAbaixoDosInscritosLancaRegraDeNegocio` | ✅ |
| 06 | Consumo de API externa | Romulo | `integracao/enderecos/EnderecoClient.java` | | ⬜ |
| 07 | Configurações e profiles | Romulo | `src/main/resources/application.properties` | | ⬜ |
| 08 | Liveness e readiness | Romulo | `platform/health/` | | ⬜ |
| 09 | Tolerância a falhas | Romulo | `integracao/enderecos/EnderecoService.java` | | ⬜ |
| 10 | Métrica de negócio | Romulo | `platform/metrics/EventoMetricas.java` | | ⬜ |
| 11 | Entidade JPA | Base | `evento/repository/Evento.java` | `EventoRepositoryTest` | ✅ |
| 12 | CRUD Panache Repository | Felipe | `evento/repository/EventoRepository.java`, `evento/domain/EventoService.java` | `listAll`, `findByIdOptional`, `persist`, `delete` do `PanacheRepository`; Postman "01-02 CRUD de eventos"; `EventoServiceTest` (criar, atualizar, remover) | ✅ |
| 13 | Transações e atualização | Felipe | `evento/domain/EventoService.java` | `@Transactional` em criar/atualizar/remover/inscrever; Postman "13 PUT → GET"; `EventoResourceTest.putAlteraODadoConsultadoDepois` | ✅ |
| 14 | Bean Validation | Wellington | `evento/boundary/EventoRequest.java` | | ⬜ |
| 15 | ExceptionMapper personalizado | Wellington | `platform/erro/` | | ⬜ |
| 16 | Resposta de erro padronizada | Wellington | `platform/erro/ErroResposta.java` | | ⬜ |

## Notas por requisito

### 03 — Lombok

<!-- O que @Getter/@Setter/@NoArgsConstructor/@Builder geram e por que não usamos @Data na entidade. -->

### 07 — Profiles

<!-- Como rodar em dev e em prod, e qual variável de ambiente sobrescreve qual chave. -->

### 08 — Quando cada health check fica DOWN

<!-- Liveness: ...  Readiness: ... -->

### 09 — Serviço externo UP/DOWN

<!-- Resposta com o mock no ar e com o mock parado (ou MOCK_FALHAR=true). -->
