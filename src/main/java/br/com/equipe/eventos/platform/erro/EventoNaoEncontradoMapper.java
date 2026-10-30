package br.com.equipe.eventos.platform.erro;

import br.com.equipe.eventos.evento.domain.EventoNaoEncontradoException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.OffsetDateTime;
import java.util.List;

/** {@link EventoNaoEncontradoException} → 404 com {@link ErroResposta}. */
@Provider
public class EventoNaoEncontradoMapper implements ExceptionMapper<EventoNaoEncontradoException> {

  @Override
  public Response toResponse(final EventoNaoEncontradoException exception) {
    return Response.status(Response.Status.NOT_FOUND)
        .entity(
            ErroResposta.builder()
                .codigo("EVENTO_NAO_ENCONTRADO")
                .mensagem(exception.getMessage())
                .timestamp(OffsetDateTime.now())
                .campos(List.of())
                .build())
        .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
        .build();
  }
}
