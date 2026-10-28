package br.com.equipe.eventos.evento.boundary;

import br.com.equipe.eventos.evento.repository.Evento;
import java.time.LocalDateTime;

/** Representação JSON de um evento devolvida pela API. */
public record EventoResponse(
    Long id,
    String nome,
    String descricao,
    String cep,
    LocalDateTime dataHora,
    int capacidade,
    int inscritos,
    int vagasDisponiveis) {

  public static EventoResponse de(final Evento evento) {
    return new EventoResponse(
        evento.getId(),
        evento.getNome(),
        evento.getDescricao(),
        evento.getCep(),
        evento.getDataHora(),
        evento.getCapacidade(),
        evento.getInscritos(),
        evento.getCapacidade() - evento.getInscritos());
  }
}
