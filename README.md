# Eventos API — Projeto final Introdução ao Quarkus

Projeto de API REST para **gestão de eventos com inscrições limitadas às vagas**. Cada evento
tem nome, CEP, data/hora e capacidade. O contrato prevê CRUD, recusa de inscrição em evento
lotado e endereço consultado por serviço HTTP externo.

**Estado desta branch:** entidade/repository, mock externo, validação e tratamento de erros
implementados. O CRUD/service (Felipe), a integração REST Client, resiliência, checks próprios
e métrica de negócio (Romulo) ainda precisam ser integrados. Portanto, `/eventos` não está
disponível aqui; a coleção Postman contém o roteiro final, não resultados já aprovados.
Veja a [matriz dos requisitos 01 a 16](docs/evidencias.md) para evidências e pendências reais.

## Equipe

| Integrante | Parte |
|------------|-------|
| Felipe | CRUD, regra de inscrição e transações |
| Romulo | Integração externa, resiliência, profiles, health e métricas |
| Wellington | Validação, tratamento de erros, Lombok, Postman e evidências |

Contratos entre as partes: [docs/divisao-do-trabalho.md](docs/divisao-do-trabalho.md).

## Stack e organização

Java 21, Quarkus 3.40.1.1, Maven wrapper, Hibernate ORM/Panache e H2 sem Docker. O `pom.xml`
também inclui REST/Jackson, REST Client, Hibernate Validator, Health, Fault Tolerance,
Micrometer/Prometheus, OpenAPI e Lombok. Ter uma extensão instalada não significa que o
requisito de negócio correspondente já esteja implementado.

A organização é Package by Feature + Boundary/Domain/Repository:

| Local | Conteúdo atual |
|-------|----------------|
| `src/main/java/br/com/equipe/eventos/evento/boundary` | Records de entrada/saída |
| `src/main/java/br/com/equipe/eventos/evento/domain` | Exceções de domínio |
| `src/main/java/br/com/equipe/eventos/evento/repository` | Entidade JPA e Panache Repository |
| `src/main/java/br/com/equipe/eventos/platform/erro` | Contrato JSON, três providers e handlers técnicos |
| `mock-enderecos` | Outro app Quarkus que simula o serviço HTTP externo |
| `src/test/java` | Persistência e testes RestAssured dos erros |

H2 atende ao enunciado sem exigir servidor de banco ou Docker. Em dev/test é em memória e
recriado; em prod usa arquivo em `data`, preservado entre reinícios. O esquema usa
`drop-and-create` em dev/test e `update` em prod.

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

| Chave existente | dev/test | prod | Variável de ambiente |
|----------------|----------|------|----------------------|
| `quarkus.datasource.jdbc.url` | `jdbc:h2:mem:eventos;DB_CLOSE_DELAY=-1` | `jdbc:h2:file:./data/eventos;DB_CLOSE_DELAY=-1` | `QUARKUS_DATASOURCE_JDBC_URL` |
| `quarkus.hibernate-orm.schema-management.strategy` | `drop-and-create` | `update` | `QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY` |
| `quarkus.http.port` | 8080 (testes: aleatória) | 8080 | `QUARKUS_HTTP_PORT` |
| `mock.falhar` (mock separado) | `false` | `false` | `MOCK_FALHAR` |

A configuração própria da API com valores por profile ainda depende de Romulo.
Não há chave de manutenção ou URL do REST Client configurada nesta branch.

### Simular indisponibilidade externa

Encerre o mock e reinicie no mesmo terminal:

```powershell
$env:MOCK_FALHAR = "true"
.\mvnw.cmd -f .\mock-enderecos\pom.xml quarkus:dev
# Após encerrar o mock:
Remove-Item Env:\MOCK_FALHAR
```

O mock retorna 503 nesse modo; sem a variável, CEP conhecido retorna 200 e desconhecido 404.
Também é possível parar o mock completamente para simular falha de conexão. O fallback
pela API de eventos só poderá ser demonstrado após integrar a implementação de Romulo.

## Contrato de eventos após integração

| Método | URI | Resposta prevista |
|--------|-----|-------------------|
| GET | `/eventos` | 200, lista |
| GET | `/eventos/{id}` | 200 ou 404 |
| POST | `/eventos` | 201 + `Location`, ou 400 |
| PUT | `/eventos/{id}` | 200, 400, 404 ou 409 |
| DELETE | `/eventos/{id}` | 204 sem corpo, ou 404 |
| POST | `/eventos/{id}/inscricoes` | 200, 404 ou 409 se lotado |
| GET | `/eventos/{id}/endereco` | Endereço externo ou fallback |

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
oito dígitos, data/hora deve ser futura e capacidade positiva. No CRUD definitivo, POST e PUT
devem receber `@NotNull @Valid EventoRequest`.

Os erros REST têm `codigo`, `mensagem`, `timestamp` ISO com offset e `campos` (lista vazia
fora da validação). Validação retorna 400, evento ausente 404 e regra de negócio 409,
preservando seu código. JSON inválido e erros HTTP também seguem o contrato; erros inesperados
retornam 500 sem expor detalhes internos e são registrados no servidor.

## Health, métricas e demonstrações

`/q/health/live`, `/q/health/ready` e `/q/metrics` são fornecidos pelas extensões.
Health agregado não comprova checks próprios; Prometheus acessível não comprova contador
de negócio. Os checks controláveis e `eventos_inscricoes_total` dependem de Romulo.

Importe a [coleção Postman](postman/projeto-final.postman_collection.json).
Após integrar as demais partes, execute CRUD → regra/métricas → erros → externo/health/profiles
→ limpeza. A coleção cria um evento próprio, guarda o id e gera datas futuras automaticamente.
Os cenários UP/DOWN e profiles têm pré-condições manuais descritas nas pastas e nas
[evidências](docs/evidencias.md); não podem ser comprovados simplesmente rodando tudo uma vez.

### Demonstração ponta a ponta automatizada

```powershell
python e2e\demonstracao_e2e.py
```

O script ([e2e/demonstracao_e2e.py](e2e/demonstracao_e2e.py)) só usa a biblioteca padrão do
Python e exige JDK 21. Ele empacota e sobe API e mock em profile prod, depois percorre CRUD,
regras, concorrência, métrica, erros, externo UP/DOWN com fallback, health, persistência após
reinício e limpeza, gerando `target/e2e/relatorio.md`. Recursos ainda não integrados aparecem
como PENDENTE. Opções e resultados: [evidências](docs/evidencias.md#demonstração-ponta-a-ponta-script-python).

## Testes e formatação

```powershell
.\mvnw.cmd test
.\mvnw.cmd -f .\mock-enderecos\pom.xml test
.\mvnw.cmd spotless:check
.\mvnw.cmd spotless:apply
```

RestAssured cobre validação POST/PUT, ausências e limites, erros 400/404/409, JSON inválido,
rota inexistente, 405/415 e 500 padronizado. A entidade tem teste de persistência; o mock tem
testes de CEP conhecido/desconhecido. Os testes dos mappers usam um recurso HTTP exclusivo de
testes e não substituem a verificação end-to-end do CRUD feita pela demonstração Python.

Detalhes do Lombok, justificativa para não usar `@Data` na entidade, matriz dos 16 requisitos e
roteiro de verificação: [docs/evidencias.md](docs/evidencias.md).
