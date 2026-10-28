package br.com.equipe.eventos.platform.erro;

import br.com.equipe.eventos.evento.domain.RegraDeNegocioException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/** {@link RegraDeNegocioException} → 409 com o código da exceção. */
// TODO (Wellington): anotar com @Provider quando implementar.
public class RegraDeNegocioMapper implements ExceptionMapper<RegraDeNegocioException> {

  @Override
  public Response toResponse(final RegraDeNegocioException exception) {
    // TODO (Wellington): 409 + ErroResposta(codigo = exception.getCodigo()).
    throw new UnsupportedOperationException("TODO");
  }
}
