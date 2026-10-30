package br.com.equipe.eventos.platform.erro;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.com.equipe.eventos.evento.boundary.EventoRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ErroRespostaTest {
  @Test
  void retorna400ComCamposInvalidosNoPostENoPut() {
    final var request =
        new EventoRequest(" ", "a".repeat(501), "01310-100", LocalDateTime.now().minusDays(1), 0);

    for (final var metodo : new String[] {"POST", "PUT"}) {
      final var resposta =
          given()
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .when()
              .request(metodo, "/teste-erros")
              .then()
              .statusCode(400)
              .body("codigo", is("REQUISICAO_INVALIDA"))
              .body("campos", hasItem(startsWith("nome: ")))
              .body("campos", hasItem(startsWith("descricao: ")))
              .body("campos", hasItem(startsWith("cep: ")))
              .body("campos", hasItem(startsWith("dataHora: ")))
              .body("campos", hasItem(startsWith("capacidade: ")));
      verificarContrato(resposta);
    }
  }

  @Test
  void rejeitaCamposObrigatoriosAusentes() {
    final var resposta =
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{}")
            .when()
            .post("/teste-erros")
            .then()
            .statusCode(400)
            .body("campos", hasSize(4));
    verificarContrato(resposta);
  }

  @Test
  void rejeitaNomeAcimaDoLimite() {
    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            new EventoRequest(
                "a".repeat(121), null, "01310100", LocalDateTime.now().plusDays(1), 1))
        .when()
        .post("/teste-erros")
        .then()
        .statusCode(400)
        .body("campos", hasSize(1))
        .body("campos", hasItem(startsWith("nome: ")));
  }

  @Test
  void aceitaLimitesValidosEDescricaoOpcional() {
    for (final var descricao : new String[] {null, "a".repeat(500)}) {
      given()
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              new EventoRequest(
                  "a".repeat(120), descricao, "01310100", LocalDateTime.now().plusDays(1), 1))
          .when()
          .post("/teste-erros")
          .then()
          .statusCode(200);
    }
  }

  @Test
  void retorna404PadronizadoParaEventoAusente() {
    final var resposta =
        given()
            .when()
            .get("/teste-erros/ausente")
            .then()
            .statusCode(404)
            .body("codigo", is("EVENTO_NAO_ENCONTRADO"))
            .body("mensagem", is("Evento 999999 não encontrado."))
            .body("campos", empty());
    verificarContrato(resposta);
  }

  @Test
  void retorna409PreservandoCodigoDaRegra() {
    final var resposta =
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/teste-erros/lotado")
            .then()
            .statusCode(409)
            .body("codigo", is("EVENTO_LOTADO"))
            .body("mensagem", is("Evento sem vagas disponiveis."))
            .body("campos", empty());
    verificarContrato(resposta);
  }

  private void verificarContrato(final ValidatableResponse resposta) {
    final var json =
        resposta
            .contentType(MediaType.APPLICATION_JSON)
            .body("mensagem", not(emptyString()))
            .extract()
            .jsonPath();
    assertEquals(
        java.util.Set.of("codigo", "mensagem", "timestamp", "campos"), json.getMap("").keySet());
    assertNotNull(OffsetDateTime.parse(json.getString("timestamp")));
  }
}
