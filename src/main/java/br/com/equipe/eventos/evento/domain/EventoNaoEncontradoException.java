package br.com.equipe.eventos.evento.domain;

/** Nenhum evento com o id pedido. Mapeada para 404 por um ExceptionMapper. */
public class EventoNaoEncontradoException extends RuntimeException {

  public EventoNaoEncontradoException(final Long id) {
    super("Evento " + id + " não encontrado.");
  }
}
