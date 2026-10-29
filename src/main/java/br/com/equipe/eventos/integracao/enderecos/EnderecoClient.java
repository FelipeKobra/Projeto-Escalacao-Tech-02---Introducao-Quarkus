package br.com.equipe.eventos.integracao.enderecos;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client do serviço de endereços. A URL vem de {@code quarkus.rest-client.enderecos-api.url}
 */
@Path("/enderecos")
@RegisterRestClient(configKey = "enderecos-api")
public interface EnderecoClient {

  @GET
  @Path("/{cep}")
  @Produces(MediaType.APPLICATION_JSON)
  EnderecoExterno buscar(@PathParam("cep") String cep);
}
