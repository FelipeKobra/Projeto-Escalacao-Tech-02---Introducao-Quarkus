package br.com.equipe.eventos.platform.erro;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.List;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Mantem o contrato tambem nos erros HTTP, de desserializacao e inesperados do Quarkus REST. */
@ApplicationScoped
public class ErrosTecnicosMapper {
  private static final Logger LOG = Logger.getLogger(ErrosTecnicosMapper.class);

  @ServerExceptionMapper
  public Response http(final WebApplicationException exception) {
    final int status = exception.getResponse().getStatus();
    if (status >= 500) {
      LOG.error("Falha HTTP no processamento da requisicao", exception);
    }
    return Response.fromResponse(exception.getResponse())
        .type(MediaType.APPLICATION_JSON)
        .entity(
            erro(
                status == 400 ? "REQUISICAO_INVALIDA" : "HTTP_" + status,
                status >= 500
                    ? "Nao foi possivel processar a requisicao."
                    : "Requisicao nao pode ser atendida (HTTP " + status + ")."))
        .build();
  }

  // Substitui o mapper mais especifico do Jackson, que usa outro formato de resposta.
  @ServerExceptionMapper(priority = Priorities.USER - 1)
  public Response estruturaJson(final MismatchedInputException exception) {
    return json(exception);
  }

  @ServerExceptionMapper
  public Response json(final JsonProcessingException exception) {
    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(
            erro("REQUISICAO_INVALIDA", "Corpo JSON invalido ou incompativel com a requisicao."))
        .build();
  }

  @ServerExceptionMapper
  public Response inesperado(final Exception exception) {
    LOG.error("Falha inesperada no processamento da requisicao", exception);
    return Response.serverError()
        .type(MediaType.APPLICATION_JSON)
        .entity(erro("ERRO_INTERNO", "Nao foi possivel processar a requisicao."))
        .build();
  }

  private ErroResposta erro(final String codigo, final String mensagem) {
    return ErroResposta.builder()
        .codigo(codigo)
        .mensagem(mensagem)
        .timestamp(OffsetDateTime.now())
        .campos(List.of())
        .build();
  }
}
