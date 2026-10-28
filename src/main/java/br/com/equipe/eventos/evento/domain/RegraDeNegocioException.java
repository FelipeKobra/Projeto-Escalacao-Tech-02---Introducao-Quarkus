package br.com.equipe.eventos.evento.domain;

import lombok.Getter;

/**
 * Violação de uma regra de negócio (ex.: evento lotado). Carrega um {@code codigo} estável para o
 * cliente comparar sem interpretar a mensagem. Mapeada para 409 por um ExceptionMapper.
 */
@Getter
public class RegraDeNegocioException extends RuntimeException {

  private final String codigo;

  public RegraDeNegocioException(final String codigo, final String mensagem) {
    super(mensagem);
    this.codigo = codigo;
  }
}
