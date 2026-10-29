package br.com.equipe.eventos.platform.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 * Readiness: a app pode receber tráfego. Condição controlável (ex.: {@code eventos.manutencao=true}
 * via config/variável de ambiente) deixa DOWN sem derrubar o processo.
 */
// TODO (Romulo): anotar com @Readiness quando implementar.
@ApplicationScoped
public class ProntidaoReadinessCheck implements HealthCheck {

  @Override
  public HealthCheckResponse call() {
    // TODO (Romulo): ler a condição com @ConfigProperty e responder up/down.
    throw new UnsupportedOperationException("TODO");
  }
}
