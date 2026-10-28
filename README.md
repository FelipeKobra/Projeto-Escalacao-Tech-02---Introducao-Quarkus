# Eventos API — Projeto final Introdução ao Quarkus

API REST para **gestão de eventos com inscrições limitadas às vagas**. Cada evento tem nome, CEP,
data/hora e capacidade; a API controla as inscrições e recusa inscrição em evento lotado. O
endereço do evento vem de um serviço HTTP externo (`mock-enderecos`), consultado por REST Client
com retry e fallback.

## Equipe

| Integrante | Parte (ver [docs/divisao-do-trabalho.md](docs/divisao-do-trabalho.md)) |
|------------|------------------------------------------------------------------------|
| Felipe     | CRUD de eventos, regra de inscrição e transações                       |
| Romulo     | Integração externa, resiliência, profiles, health e métricas           |
| Wellington | Validação, tratamento de erros, Lombok, Postman e evidências           |

## Stack

- **Java 21** + **Quarkus 3.40.1.1** (LTS), gerado em [code.quarkus.io](https://code.quarkus.io)
- Maven (wrapper incluso: `./mvnw`, não precisa instalar Maven)
- Hibernate ORM + Panache sobre **H2** (relacional, sem Docker)
- Quarkus REST + Jackson, REST Client, Hibernate Validator, SmallRye Health, SmallRye Fault
  Tolerance, Micrometer (Prometheus), SmallRye OpenAPI, Lombok

### Por que H2 e não PostgreSQL/Docker

O enunciado aceita H2 e não dá ponto extra para PostgreSQL ou Docker Compose. Com H2 ninguém da
equipe (nem quem corrige) precisa instalar ou subir nada além do JDK 21: em `dev`/`test` o banco é
em memória e recarrega `import.sql`; em `prod` é um arquivo em `./data/` que sobrevive ao restart.

## Estrutura

Package by Feature + Boundary / Domain / Repository (Aula 05):

```
src/main/java/br/com/equipe/eventos/
├── evento/
│   ├── boundary/      # EventoResource e DTOs HTTP (records)
│   ├── domain/        # EventoService, regras de negócio e exceções
│   └── repository/    # Evento (@Entity) e EventoRepository (PanacheRepository)
├── integracao/
│   └── enderecos/     # REST Client do mock-enderecos + fallback
└── platform/
    ├── erro/          # ErroResposta + ExceptionMappers
    ├── health/        # liveness e readiness
    └── metrics/       # métrica de negócio (Micrometer)
mock-enderecos/        # serviço externo simulado (outro app Quarkus, porta 8081)
```

## Como rodar

Pré-requisito: **JDK 21** (`java -version`). No Windows use `mvnw.cmd` no lugar de `./mvnw`.

```bash
# 1) Serviço externo simulado (terminal 1) — porta 8081
./mvnw -f mock-enderecos/pom.xml quarkus:dev

# 2) API (terminal 2) — porta 8080, profile dev
./mvnw quarkus:dev
```

- Swagger UI: http://localhost:8080/q/swagger-ui
- Dev UI: http://localhost:8080/q/dev-ui

### Profile prod

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
# sobrescrevendo uma configuração por variável de ambiente:
QUARKUS_DATASOURCE_JDBC_URL="jdbc:h2:file:./data/outro;DB_CLOSE_DELAY=-1" java -jar target/quarkus-app/quarkus-run.jar
```

### Simular o serviço externo fora do ar

Pare o terminal 1, **ou** suba o mock respondendo 503 em toda consulta:

```bash
MOCK_FALHAR=true ./mvnw -f mock-enderecos/pom.xml quarkus:dev
```

### Testes e formatação

```bash
./mvnw test                                  # testes da API
./mvnw -f mock-enderecos/pom.xml test        # testes do mock
./mvnw spotless:apply                        # formata o código (Google Java Format) antes do commit
```

## Endpoints

| Método | URI                           | Descrição                         |
|--------|-------------------------------|-----------------------------------|
| GET    | `/eventos`                    | Lista eventos                     |
| GET    | `/eventos/{id}`               | Busca evento                      |
| POST   | `/eventos`                    | Cria evento (201 + `Location`)    |
| PUT    | `/eventos/{id}`               | Atualiza evento                   |
| DELETE | `/eventos/{id}`               | Remove evento (204)               |
| POST   | `/eventos/{id}/inscricoes`    | Inscreve uma pessoa (409 se lotado) |
| GET    | `/eventos/{id}/endereco`      | Endereço via serviço externo      |
| GET    | `/q/health/live`              | Liveness                          |
| GET    | `/q/health/ready`             | Readiness                         |
| GET    | `/q/metrics`                  | Métricas Prometheus               |

> Exemplos de requisição: coleção [postman/projeto-final.postman_collection.json](postman/projeto-final.postman_collection.json).

## Configurações e profiles

<!-- TODO (Romulo): listar as chaves próprias (@ConfigProperty), valores por profile e as variáveis de ambiente. -->

| Chave | dev | prod | Variável de ambiente |
|-------|-----|------|----------------------|
| `quarkus.datasource.jdbc.url` | `jdbc:h2:mem:eventos` | `jdbc:h2:file:./data/eventos` | `QUARKUS_DATASOURCE_JDBC_URL` |

## Evidências dos requisitos

Tabela completa com arquivo, requisição e resultado de cada requisito: [docs/evidencias.md](docs/evidencias.md).
