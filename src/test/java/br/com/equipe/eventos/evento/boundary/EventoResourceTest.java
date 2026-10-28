package br.com.equipe.eventos.evento.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Contrato HTTP de {@code /eventos}. Cada teste que escreve cria o próprio evento, para não
 * depender da ordem de execução nem alterar os dados do import.sql (evento 1 com vagas, evento 2
 * lotado).
 */
@QuarkusTest
class EventoResourceTest {

  private static final long EVENTO_LOTADO = 2L;
  private static final long ID_INEXISTENTE = 999_999L;

  private static String corpo(final String nome, final int capacidade) {
    return """
        {
          "nome": "%s",
          "descricao": "Evento criado no teste",
          "cep": "01310100",
          "dataHora": "2026-12-01T10:00:00",
          "capacidade": %d
        }
        """
        .formatted(nome, capacidade);
  }

  private static long criarEvento(final int capacidade) {
    return given()
        .contentType(ContentType.JSON)
        .body(corpo("Evento de teste", capacidade))
        .when()
        .post("/eventos")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  @Test
  void listarResponde200ComOsEventos() {
    given()
        .when()
        .get("/eventos")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("size()", greaterThanOrEqualTo(2));
  }

  @Test
  void buscarPorIdResponde200() {
    given()
        .when()
        .get("/eventos/1")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("id", equalTo(1))
        .body("nome", equalTo("Workshop de Quarkus"));
  }

  @Test
  void criarResponde201ComLocationEInscritosZerados() {
    // Act
    final String location =
        given()
            .contentType(ContentType.JSON)
            .body(corpo("Novo evento", 15))
            .when()
            .post("/eventos")
            .then()
            .statusCode(201)
            .header("Location", matchesPattern(".*/eventos/\\d+$"))
            .body("nome", equalTo("Novo evento"))
            .body("inscritos", equalTo(0))
            .body("vagasDisponiveis", equalTo(15))
            .extract()
            .header("Location");

    // Assert: o Location aponta para um recurso consultável.
    given().when().get(location).then().statusCode(200).body("nome", equalTo("Novo evento"));
  }

  @Test
  void putAlteraODadoConsultadoDepois() {
    // Arrange
    final long id = criarEvento(10);

    // Act
    given()
        .contentType(ContentType.JSON)
        .body(corpo("Nome alterado", 25))
        .when()
        .put("/eventos/" + id)
        .then()
        .statusCode(200)
        .body("nome", equalTo("Nome alterado"));

    // Assert
    given()
        .when()
        .get("/eventos/" + id)
        .then()
        .statusCode(200)
        .body("nome", equalTo("Nome alterado"))
        .body("capacidade", equalTo(25));
  }

  @Test
  void removerResponde204() {
    // Arrange
    final long id = criarEvento(10);

    // Act / Assert
    given().when().delete("/eventos/" + id).then().statusCode(204);
  }

  @Test
  void inscreverComVagasResponde200ComOEventoAtualizado() {
    // Arrange
    final long id = criarEvento(3);

    // Act / Assert
    given()
        .when()
        .post("/eventos/" + id + "/inscricoes")
        .then()
        .statusCode(200)
        .body("inscritos", equalTo(1))
        .body("vagasDisponiveis", equalTo(2));
  }

  @Test
  @Disabled("Aguardando o ExceptionMapper de EventoNaoEncontradoException (Wellington) → 404")
  void buscarIdInexistenteResponde404() {
    given().when().get("/eventos/" + ID_INEXISTENTE).then().statusCode(404);
  }

  @Test
  @Disabled("Aguardando o ExceptionMapper de EventoNaoEncontradoException (Wellington) → 404")
  void removerIdInexistenteResponde404() {
    given().when().delete("/eventos/" + ID_INEXISTENTE).then().statusCode(404);
  }

  @Test
  @Disabled("Aguardando o ExceptionMapper de RegraDeNegocioException (Wellington) → 409")
  void inscreverEmEventoLotadoResponde409() {
    given()
        .when()
        .post("/eventos/" + EVENTO_LOTADO + "/inscricoes")
        .then()
        .statusCode(409)
        .body("codigo", equalTo("EVENTO_LOTADO"));
  }

  @Test
  @Disabled("Aguardando o ExceptionMapper de RegraDeNegocioException (Wellington) → 409")
  void putComCapacidadeAbaixoDosInscritosResponde409() {
    given()
        .contentType(ContentType.JSON)
        .body(corpo("Meetup Java", 1))
        .when()
        .put("/eventos/" + EVENTO_LOTADO)
        .then()
        .statusCode(409)
        .body("codigo", equalTo("CAPACIDADE_ABAIXO_DOS_INSCRITOS"));
  }
}
