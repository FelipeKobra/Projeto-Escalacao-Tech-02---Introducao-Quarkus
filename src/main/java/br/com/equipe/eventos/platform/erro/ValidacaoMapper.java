package br.com.equipe.eventos.platform.erro;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/** Falha de Bean Validation → 400 com a lista de campos inválidos. */
// TODO (Wellington): anotar com @Provider quando implementar.
public class ValidacaoMapper implements ExceptionMapper<ConstraintViolationException> {

  @Override
  public Response toResponse(final ConstraintViolationException exception) {
    // TODO (Wellington): 400 + ErroResposta(codigo "REQUISICAO_INVALIDA", campos = violações).
    throw new UnsupportedOperationException("TODO");
  }
}
