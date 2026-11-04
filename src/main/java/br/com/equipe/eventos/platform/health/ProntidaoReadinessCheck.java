package br.com.equipe.eventos.platform.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness: a app pode receber tráfego. Condição controlável (ex.: {@code eventos.manutencao=true}
 * via config/variável de ambiente) deixa DOWN sem derrubar o processo.
 */
@Readiness
@ApplicationScoped
public class ProntidaoReadinessCheck implements HealthCheck {

  private final ReadinessService readinessService;
  private final String app;

  public ProntidaoReadinessCheck(
      ReadinessService readinessService, @ConfigProperty(name = "eventos.app") String app) {
    this.readinessService = readinessService;
    this.app = app;
  }

  @Override
  public HealthCheckResponse call() {
    if (readinessService.isUp()) {
      return HealthCheckResponse.up(app);
    }
    return HealthCheckResponse.down(app);
  }
}
