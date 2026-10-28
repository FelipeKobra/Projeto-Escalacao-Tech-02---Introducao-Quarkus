package br.com.equipe.mock.enderecos;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Serviço externo simulado: consulta de endereço por CEP. Roda como processo separado (porta 8081)
 * para que a eventos-api faça uma chamada HTTP real. Para demonstrar falha: pare o processo, ou
 * suba com {@code MOCK_FALHAR=true} para responder 503 em toda consulta.
 */
@Path("/enderecos")
@Produces(MediaType.APPLICATION_JSON)
public class EnderecoResource {

  private static final Map<String, Endereco> ENDERECOS =
      Map.of(
          "01310100",
          new Endereco("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP"),
          "20040002",
          new Endereco("20040002", "Rua da Assembleia", "Centro", "Rio de Janeiro", "RJ"),
          "30130010",
          new Endereco("30130010", "Avenida Afonso Pena", "Centro", "Belo Horizonte", "MG"),
          "70040010",
          new Endereco(
              "70040010",
              "Esplanada dos Ministérios",
              "Zona Cívico-Administrativa",
              "Brasília",
              "DF"));

  @ConfigProperty(name = "mock.falhar", defaultValue = "false")
  boolean falhar;

  @GET
  @Path("/{cep}")
  public Response buscar(@PathParam("cep") final String cep) {
    if (falhar) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
    final Endereco endereco = ENDERECOS.get(cep);
    if (null == endereco) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    return Response.ok(endereco).build();
  }
}
