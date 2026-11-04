package br.com.equipe.eventos.platform.erro;

import br.com.equipe.eventos.evento.boundary.EventoRequest;
import br.com.equipe.eventos.evento.domain.EventoNaoEncontradoException;
import br.com.equipe.eventos.evento.domain.RegraDeNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Recurso somente de testes, independente do CRUD desenvolvido em outra branch. */
@Path("/teste-erros")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ErroResource {
  @POST
  public EventoRequest criar(@NotNull @Valid final EventoRequest request) {
    return request;
  }

  @PUT
  public EventoRequest atualizar(@NotNull @Valid final EventoRequest request) {
    return request;
  }

  @GET
  @Path("/ausente")
  public void ausente() {
    throw new EventoNaoEncontradoException(999999L);
  }

  @POST
  @Path("/lotado")
  public void lotado() {
    throw new RegraDeNegocioException("EVENTO_LOTADO", "Evento sem vagas disponiveis.");
  }

  @GET
  @Path("/inesperado")
  public void inesperado() {
    throw new IllegalStateException("Detalhe interno que nao deve ser enviado ao cliente");
  }

  @GET
  @Path("/metodo")
  public void metodo() {
    throw new NotAllowedException("POST", new String[] {"PUT"});
  }
}
