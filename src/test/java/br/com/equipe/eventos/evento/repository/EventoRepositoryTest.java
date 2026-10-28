package br.com.equipe.eventos.evento.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EventoRepositoryTest {

  @Inject EventoRepository repository;

  @Test
  @TestTransaction
  void persisteEConsultaEvento() {
    // Arrange
    final Evento evento = new Evento();
    evento.setNome("Evento de teste");
    evento.setCep("01310100");
    evento.setDataHora(LocalDateTime.of(2026, 12, 1, 10, 0));
    evento.setCapacidade(10);

    // Act
    repository.persist(evento);
    final Evento salvo = repository.findById(evento.getId());

    // Assert
    assertNotNull(salvo.getId());
    assertEquals("Evento de teste", salvo.getNome());
    assertEquals(0, salvo.getInscritos());
  }
}
