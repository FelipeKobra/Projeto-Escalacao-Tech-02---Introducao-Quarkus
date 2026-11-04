package br.com.equipe.eventos.platform.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/** Liveness: o processo está vivo e respondendo. DOWN só se a app travou e precisa reiniciar. */
@Liveness
@ApplicationScoped
public class AplicacaoLivenessCheck implements HealthCheck {

  private final String app;

  public AplicacaoLivenessCheck(@ConfigProperty(name = "eventos.app") String app) {
    this.app = app;
  }

  @Override
  public HealthCheckResponse call() {
    return HealthCheckResponse.up(app);
  }
}
