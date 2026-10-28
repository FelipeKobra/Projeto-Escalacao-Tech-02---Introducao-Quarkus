# Evidências dos requisitos

Cada linha aponta o código e a requisição (coleção Postman) ou o teste que demonstra o requisito.
Preencher conforme as partes forem integradas. Quem consolida: Wellington.

| # | Requisito | Responsável | Código | Requisição Postman / teste | Status |
|---|-----------|-------------|--------|----------------------------|--------|
| 01 | Recursos e métodos HTTP | Felipe | `evento/boundary/EventoResource.java` | | ⬜ |
| 02 | Códigos de status e contratos | Felipe | `evento/boundary/EventoResource.java` | | ⬜ |
| 03 | Lombok aplicado conscientemente | Wellington | `evento/repository/Evento.java`, `platform/erro/ErroResposta.java` | | ⬜ |
| 04 | API Quarkus/JAX-RS | Felipe | `evento/boundary/EventoResource.java` | | ⬜ |
| 05 | CDI e regra de negócio | Felipe | `evento/domain/EventoService.java` | | ⬜ |
| 06 | Consumo de API externa | Romulo | `integracao/enderecos/EnderecoClient.java` | | ⬜ |
| 07 | Configurações e profiles | Romulo | `src/main/resources/application.properties` | | ⬜ |
| 08 | Liveness e readiness | Romulo | `platform/health/` | | ⬜ |
| 09 | Tolerância a falhas | Romulo | `integracao/enderecos/EnderecoService.java` | | ⬜ |
| 10 | Métrica de negócio | Romulo | `platform/metrics/EventoMetricas.java` | | ⬜ |
| 11 | Entidade JPA | Base | `evento/repository/Evento.java` | `EventoRepositoryTest` | ✅ |
| 12 | CRUD Panache Repository | Felipe | `evento/repository/EventoRepository.java`, `evento/domain/EventoService.java` | | ⬜ |
| 13 | Transações e atualização | Felipe | `evento/domain/EventoService.java` | | ⬜ |
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
