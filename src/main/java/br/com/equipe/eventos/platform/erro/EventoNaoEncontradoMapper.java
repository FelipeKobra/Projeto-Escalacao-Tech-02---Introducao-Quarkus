package br.com.equipe.eventos.platform.erro;

import br.com.equipe.eventos.evento.domain.EventoNaoEncontradoException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/** {@link EventoNaoEncontradoException} → 404 com {@link ErroResposta}. */
// TODO (Wellington): anotar com @Provider quando implementar.
public class EventoNaoEncontradoMapper implements ExceptionMapper<EventoNaoEncontradoException> {

  @Override
  public Response toResponse(final EventoNaoEncontradoException exception) {
    // TODO (Wellington): 404 + ErroResposta(codigo "EVENTO_NAO_ENCONTRADO").
    throw new UnsupportedOperationException("TODO");
  }
}
