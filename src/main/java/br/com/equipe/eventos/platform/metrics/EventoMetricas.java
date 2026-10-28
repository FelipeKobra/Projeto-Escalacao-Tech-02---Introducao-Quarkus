package br.com.equipe.eventos.platform.metrics;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Métricas de negócio (Micrometer). O contador de inscrições aparece em {@code /q/metrics} como
 * {@code eventos_inscricoes_total}.
 */
@ApplicationScoped
public class EventoMetricas {

  // TODO (Romulo): receber MeterRegistry no construtor e registrar um Counter "eventos.inscricoes".

  public void registrarInscricao() {
    // TODO (Romulo): counter.increment(); chamar no EventoService.inscrever depois do merge do
    // Felipe.
    throw new UnsupportedOperationException("TODO");
  }
}
