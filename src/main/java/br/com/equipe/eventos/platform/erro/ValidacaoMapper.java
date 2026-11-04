package br.com.equipe.eventos.platform.erro;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.OffsetDateTime;

/** Falha de Bean Validation → 400 com a lista de campos inválidos. */
@Provider
public class ValidacaoMapper implements ExceptionMapper<ConstraintViolationException> {

  @Override
  public Response toResponse(final ConstraintViolationException exception) {
    final var campos =
        exception.getConstraintViolations().stream()
            .map(
                violacao -> {
                  String campo = "";
                  for (final var no : violacao.getPropertyPath()) {
                    if (no.getName() != null) {
                      campo = no.getName();
                    }
                  }
                  return campo + ": " + violacao.getMessage();
                })
            .distinct()
            .sorted()
            .toList();
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(
            ErroResposta.builder()
                .codigo("REQUISICAO_INVALIDA")
                .mensagem("Existem campos invalidos na requisicao.")
                .timestamp(OffsetDateTime.now())
                .campos(campos)
                .build())
        .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
        .build();
  }
}
