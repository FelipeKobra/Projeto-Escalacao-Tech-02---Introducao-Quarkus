package br.com.equipe.eventos.evento.boundary;

import br.com.equipe.eventos.platform.health.ReadinessService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/ready")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReadinessResource {

  private final ReadinessService readinessService;

  public ReadinessResource(ReadinessService readinessService) {
    this.readinessService = readinessService;
  }

  @GET
  @Path("/up")
  public Response up() {
    readinessService.up();
    return Response.ok().build();
  }

  @GET
  @Path("/down")
  public Response down() {
    readinessService.down();
    return Response.ok().build();
  }
}
