package br.com.equipe.eventos.evento.boundary;

import br.com.equipe.eventos.evento.domain.EventoService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

/** Interface HTTP de eventos: só traduz HTTP ↔ DTO e delega ao {@link EventoService}. */
@Path("/eventos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventoResource {

  private final EventoService service;

  public EventoResource(final EventoService service) {
    this.service = service;
  }

  @GET
  public List<EventoResponse> listar() {
    // TODO (Felipe): 200 com a lista (EventoResponse::de).
    throw new UnsupportedOperationException("TODO");
  }

  @GET
  @Path("/{id}")
  public EventoResponse buscar(@PathParam("id") final Long id) {
    // TODO (Felipe): 200 com o evento.
    throw new UnsupportedOperationException("TODO");
  }

  @POST
  public Response criar(final EventoRequest request, @Context final UriInfo uriInfo) {
    // TODO (Felipe): @Valid no request; 201 + Location (uriInfo.getAbsolutePathBuilder()...).
    throw new UnsupportedOperationException("TODO");
  }

  @PUT
  @Path("/{id}")
  public EventoResponse atualizar(@PathParam("id") final Long id, final EventoRequest request) {
    // TODO (Felipe): @Valid no request; 200 com o evento atualizado.
    throw new UnsupportedOperationException("TODO");
  }

  @DELETE
  @Path("/{id}")
  public void remover(@PathParam("id") final Long id) {
    // TODO (Felipe): método void responde 204.
    throw new UnsupportedOperationException("TODO");
  }

  @POST
  @Path("/{id}/inscricoes")
  public EventoResponse inscrever(@PathParam("id") final Long id) {
    // TODO (Felipe): 200 com o evento atualizado; 409 quando lotado (via ExceptionMapper).
    throw new UnsupportedOperationException("TODO");
  }
}
