package br.com.equipe.eventos.platform.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

/** Liveness: o processo está vivo e respondendo. DOWN só se a app travou e precisa reiniciar. */
// TODO (Romulo): anotar com @Liveness quando implementar.
@ApplicationScoped
public class AplicacaoLivenessCheck implements HealthCheck {

  @Override
  public HealthCheckResponse call() {
    // TODO (Romulo): HealthCheckResponse.up("aplicacao") (+ dados úteis com .withData).
    throw new UnsupportedOperationException("TODO");
  }
}
