package br.com.equipe.mock.enderecos;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EnderecoResourceTest {

  @Test
  void retornaEnderecoDeCepConhecido() {
    given()
        .when()
        .get("/enderecos/01310100")
        .then()
        .statusCode(200)
        .body("cidade", is("São Paulo"))
        .body("uf", is("SP"));
  }

  @Test
  void retorna404ParaCepDesconhecido() {
    given().when().get("/enderecos/00000000").then().statusCode(404);
  }
}
