package br.com.equipe.eventos.evento.domain;

import br.com.equipe.eventos.evento.boundary.EventoRequest;
import br.com.equipe.eventos.evento.repository.Evento;
import br.com.equipe.eventos.evento.repository.EventoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Regras de negócio de eventos. O resource só delega para cá; a persistência fica no {@link
 * EventoRepository}. Métodos de escrita são {@code @Transactional}.
 */
@ApplicationScoped
public class EventoService {

  private final EventoRepository repository;

  public EventoService(final EventoRepository repository) {
    this.repository = repository;
  }

  public List<Evento> listar() {
    // TODO (Felipe): devolver todos os eventos.
    throw new UnsupportedOperationException("TODO");
  }

  /** Lança {@link EventoNaoEncontradoException} se o id não existir. */
  public Evento buscar(final Long id) {
    // TODO (Felipe): repository.findByIdOptional(id).orElseThrow(...)
    throw new UnsupportedOperationException("TODO");
  }

  @Transactional
  public Evento criar(final EventoRequest request) {
    // TODO (Felipe): montar o Evento a partir do request (inscritos = 0) e persistir.
    throw new UnsupportedOperationException("TODO");
  }

  /**
   * Regra: a nova capacidade não pode ficar abaixo dos inscritos — lança {@code
   * RegraDeNegocioException("CAPACIDADE_ABAIXO_DOS_INSCRITOS", ...)}.
   */
  @Transactional
  public Evento atualizar(final Long id, final EventoRequest request) {
    // TODO (Felipe): buscar, validar a regra, copiar os campos (entidade gerenciada, sem persist).
    throw new UnsupportedOperationException("TODO");
  }

  @Transactional
  public void remover(final Long id) {
    // TODO (Felipe): buscar (404 se não existir) e remover.
    throw new UnsupportedOperationException("TODO");
  }

  /** Regra: evento lotado lança {@code RegraDeNegocioException("EVENTO_LOTADO", ...)}. */
  @Transactional
  public Evento inscrever(final Long id) {
    // TODO (Felipe): buscar, checar vagas, incrementar inscritos.
    throw new UnsupportedOperationException("TODO");
  }
}
