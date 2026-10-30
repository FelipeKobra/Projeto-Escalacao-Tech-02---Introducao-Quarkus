package br.com.equipe.eventos.platform.erro;

import br.com.equipe.eventos.evento.domain.RegraDeNegocioException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.OffsetDateTime;
import java.util.List;

/** {@link RegraDeNegocioException} → 409 com o código da exceção. */
@Provider
public class RegraDeNegocioMapper implements ExceptionMapper<RegraDeNegocioException> {

  @Override
  public Response toResponse(final RegraDeNegocioException exception) {
    return Response.status(Response.Status.CONFLICT)
        .entity(
            ErroResposta.builder()
                .codigo(exception.getCodigo())
                .mensagem(exception.getMessage())
                .timestamp(OffsetDateTime.now())
                .campos(List.of())
                .build())
        .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
        .build();
  }
}
