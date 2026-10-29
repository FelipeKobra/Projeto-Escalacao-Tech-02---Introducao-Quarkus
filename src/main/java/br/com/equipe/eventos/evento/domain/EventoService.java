package br.com.equipe.eventos.evento.domain;

import br.com.equipe.eventos.evento.boundary.EventoRequest;
import br.com.equipe.eventos.evento.repository.Evento;
import br.com.equipe.eventos.evento.repository.EventoRepository;
import br.com.equipe.eventos.integracao.enderecos.EnderecoExterno;
import br.com.equipe.eventos.integracao.enderecos.EnderecoService;
import br.com.equipe.eventos.platform.metrics.EventoMetricas;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Regras de negócio de eventos. O resource só delega para cá; a persistência fica no {@link
 * EventoRepository}. Métodos de escrita são {@code @Transactional}.
 */
@ApplicationScoped
public class EventoService {

  private final EventoRepository repository;
  private final EnderecoService enderecoService;
  private final EventoMetricas metricas;

  public EventoService(final EventoRepository repository, EnderecoService enderecoService, EventoMetricas metricas) {
    this.repository = repository;
    this.enderecoService = enderecoService;
    this.metricas = metricas;
  }

  public List<Evento> listar() {
    return repository.listAll();
  }

  /** Lança {@link EventoNaoEncontradoException} se o id não existir. */
  public Evento buscar(final Long id) {
    return repository.findByIdOptional(id).orElseThrow(() -> new EventoNaoEncontradoException(id));
  }

  @Transactional
  public Evento criar(final EventoRequest request) {
    final Evento evento = new Evento();
    copiarCampos(request, evento);
    evento.setInscritos(0);
    repository.persist(evento);
    return evento;
  }

  /**
   * Regra: a nova capacidade não pode ficar abaixo dos inscritos — lança {@code
   * RegraDeNegocioException("CAPACIDADE_ABAIXO_DOS_INSCRITOS", ...)}.
   */
  @Transactional
  public Evento atualizar(final Long id, final EventoRequest request) {
    final Evento evento = buscar(id);
    if (request.capacidade() < evento.getInscritos()) {
      throw new RegraDeNegocioException(
          "CAPACIDADE_ABAIXO_DOS_INSCRITOS",
          "A capacidade não pode ficar abaixo dos "
              + evento.getInscritos()
              + " inscritos do evento "
              + id
              + ".");
    }
    // Entidade gerenciada: as alterações vão para o banco no commit, sem persist.
    copiarCampos(request, evento);
    return evento;
  }

  @Transactional
  public void remover(final Long id) {
    repository.delete(buscar(id));
  }

  /** Regra: evento lotado lança {@code RegraDeNegocioException("EVENTO_LOTADO", ...)}. */
  @Transactional
  public Evento inscrever(final Long id) {
    // Lock pessimista: duas inscrições simultâneas não passam juntas da última vaga.
    final Evento evento =
        repository
            .findByIdOptional(id, LockModeType.PESSIMISTIC_WRITE)
            .orElseThrow(() -> new EventoNaoEncontradoException(id));
    if (evento.getInscritos() >= evento.getCapacidade()) {
      throw new RegraDeNegocioException("EVENTO_LOTADO", "O evento " + id + " está lotado.");
    }
    evento.setInscritos(evento.getInscritos() + 1);
    metricas.registrarInscricao();
    return evento;
  }

  private static void copiarCampos(final EventoRequest request, final Evento evento) {
    evento.setNome(request.nome());
    evento.setDescricao(request.descricao());
    evento.setCep(request.cep());
    evento.setDataHora(request.dataHora());
    evento.setCapacidade(request.capacidade());
  }

  public EnderecoExterno buscarEndereco(Long id) {
    Evento evento = buscar(id);
    return enderecoService.buscar(evento.getCep());
  }
}
