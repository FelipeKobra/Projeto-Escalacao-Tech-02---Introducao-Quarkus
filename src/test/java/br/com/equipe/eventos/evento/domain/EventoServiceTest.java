package br.com.equipe.eventos.evento.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.equipe.eventos.evento.boundary.EventoRequest;
import br.com.equipe.eventos.evento.repository.Evento;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Regras do {@link EventoService} contra o H2 de teste. {@code @TestTransaction} desfaz as escritas
 * ao fim de cada teste, então os dados do import.sql (evento 1 com vagas, evento 2 lotado) ficam
 * intactos.
 */
@QuarkusTest
class EventoServiceTest {

  private static final long EVENTO_LOTADO = 2L;
  private static final long ID_INEXISTENTE = 999_999L;

  @Inject EventoService service;

  private static EventoRequest request(final int capacidade) {
    return new EventoRequest(
        "Evento de teste",
        "Descrição",
        "01310100",
        LocalDateTime.of(2026, 12, 1, 10, 0),
        capacidade);
  }

  @Test
  @TestTransaction
  void criarComecaSemInscritos() {
    // Act
    final Evento criado = service.criar(request(10));

    // Assert
    assertNotNull(criado.getId());
    assertEquals(10, criado.getCapacidade());
    assertEquals(0, criado.getInscritos());
  }

  @Test
  @TestTransaction
  void listarDevolveOsEventosCadastrados() {
    // Act / Assert
    assertFalse(service.listar().isEmpty());
  }

  @Test
  void buscarIdInexistenteLancaEventoNaoEncontrado() {
    // Act / Assert
    assertThrows(EventoNaoEncontradoException.class, () -> service.buscar(ID_INEXISTENTE));
  }

  @Test
  @TestTransaction
  void inscreverComVagasIncrementaInscritos() {
    // Arrange
    final Evento evento = service.criar(request(2));

    // Act
    final Evento atualizado = service.inscrever(evento.getId());

    // Assert
    assertEquals(1, atualizado.getInscritos());
  }

  @Test
  @TestTransaction
  void inscreverEmEventoLotadoLancaEventoLotado() {
    // Act
    final RegraDeNegocioException erro =
        assertThrows(RegraDeNegocioException.class, () -> service.inscrever(EVENTO_LOTADO));

    // Assert
    assertEquals("EVENTO_LOTADO", erro.getCodigo());
  }

  @Test
  @TestTransaction
  void atualizarCopiaOsCampos() {
    // Arrange
    final Evento evento = service.criar(request(10));
    final EventoRequest novo =
        new EventoRequest(
            "Nome novo", "Outra descrição", "20040002", LocalDateTime.of(2027, 1, 5, 9, 0), 20);

    // Act
    service.atualizar(evento.getId(), novo);
    final Evento consultado = service.buscar(evento.getId());

    // Assert
    assertEquals("Nome novo", consultado.getNome());
    assertEquals("Outra descrição", consultado.getDescricao());
    assertEquals("20040002", consultado.getCep());
    assertEquals(LocalDateTime.of(2027, 1, 5, 9, 0), consultado.getDataHora());
    assertEquals(20, consultado.getCapacidade());
  }

  @Test
  @TestTransaction
  void atualizarCapacidadeAbaixoDosInscritosLancaRegraDeNegocio() {
    // Act
    final RegraDeNegocioException erro =
        assertThrows(
            RegraDeNegocioException.class, () -> service.atualizar(EVENTO_LOTADO, request(1)));

    // Assert
    assertEquals("CAPACIDADE_ABAIXO_DOS_INSCRITOS", erro.getCodigo());
  }

  @Test
  @TestTransaction
  void atualizarIdInexistenteLancaEventoNaoEncontrado() {
    // Act / Assert
    assertThrows(
        EventoNaoEncontradoException.class, () -> service.atualizar(ID_INEXISTENTE, request(10)));
  }

  @Test
  @TestTransaction
  void removerApagaOEvento() {
    // Arrange
    final Evento evento = service.criar(request(10));

    // Act
    service.remover(evento.getId());

    // Assert
    assertThrows(EventoNaoEncontradoException.class, () -> service.buscar(evento.getId()));
  }

  @Test
  @TestTransaction
  void removerIdInexistenteLancaEventoNaoEncontrado() {
    // Act / Assert
    assertThrows(EventoNaoEncontradoException.class, () -> service.remover(ID_INEXISTENTE));
  }
}
