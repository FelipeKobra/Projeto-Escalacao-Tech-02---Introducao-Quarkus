package br.com.equipe.eventos.platform.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Métricas de negócio (Micrometer). O contador de inscrições aparece em {@code /q/metrics} como
 * {@code eventos_inscricoes_total}.
 */
@ApplicationScoped
public class EventoMetricas {

  private final MeterRegistry meterRegistry;

  public EventoMetricas(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void registrarInscricao() {
    this.meterRegistry.counter("eventos.inscricoes").increment();
  }
}
