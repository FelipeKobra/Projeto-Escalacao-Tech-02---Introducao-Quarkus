package br.com.equipe.eventos.evento.boundary;

import br.com.equipe.eventos.evento.domain.EventoService;
import br.com.equipe.eventos.evento.repository.Evento;
import jakarta.validation.Valid;
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
import java.net.URI;
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
    return service.listar().stream().map(EventoResponse::de).toList();
  }

  @GET
  @Path("/{id}")
  public EventoResponse buscar(@PathParam("id") final Long id) {
    return EventoResponse.de(service.buscar(id));
  }

  @POST
  public Response criar(@Valid final EventoRequest request, @Context final UriInfo uriInfo) {
    final Evento criado = service.criar(request);
    final URI location =
        uriInfo.getAbsolutePathBuilder().path(String.valueOf(criado.getId())).build();
    return Response.created(location).entity(EventoResponse.de(criado)).build();
  }

  @PUT
  @Path("/{id}")
  public EventoResponse atualizar(
      @PathParam("id") final Long id, @Valid final EventoRequest request) {
    return EventoResponse.de(service.atualizar(id, request));
  }

  /** Método {@code void}: o Quarkus REST responde 204 No Content. */
  @DELETE
  @Path("/{id}")
  public void remover(@PathParam("id") final Long id) {
    service.remover(id);
  }

  /**
   * Sem corpo, então aceita qualquer Content-Type (o {@code @Consumes} JSON da classe responderia
   * 415). 409 quando lotado, via ExceptionMapper de {@code RegraDeNegocioException}.
   */
  @POST
  @Path("/{id}/inscricoes")
  @Consumes(MediaType.WILDCARD)
  public EventoResponse inscrever(@PathParam("id") final Long id) {
    return EventoResponse.de(service.inscrever(id));
  }
}
