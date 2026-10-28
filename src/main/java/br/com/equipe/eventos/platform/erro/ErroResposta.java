package br.com.equipe.eventos.platform.erro;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;

/**
 * Corpo JSON padrão de todo erro da API. O {@code @Builder} do Lombok gera {@code
 * ErroResposta.builder().codigo(...).mensagem(...).build()}.
 *
 * @param codigo identificador estável do erro (ex.: EVENTO_NAO_ENCONTRADO, EVENTO_LOTADO)
 * @param mensagem texto para o usuário
 * @param timestamp momento do erro
 * @param campos detalhes de validação (vazio quando não se aplica)
 */
@Builder
public record ErroResposta(
    String codigo, String mensagem, OffsetDateTime timestamp, List<String> campos) {}
