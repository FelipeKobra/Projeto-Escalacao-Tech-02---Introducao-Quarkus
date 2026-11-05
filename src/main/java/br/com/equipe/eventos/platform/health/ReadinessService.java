package br.com.equipe.eventos.platform.health;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Estado de prontidão da API. Começa UP, ou DOWN quando {@code eventos.manutencao=true} (variável
 * {@code EVENTOS_MANUTENCAO}); em execução, {@code /ready/up} e {@code /ready/down} alternam o
 * estado para demonstração.
 */
@ApplicationScoped
public class ReadinessService {

  private final AtomicBoolean ready;

  public ReadinessService(@ConfigProperty(name = "eventos.manutencao") final boolean manutencao) {
    this.ready = new AtomicBoolean(!manutencao);
  }

  public void up() {
    ready.set(true);
  }

  public void down() {
    ready.set(false);
  }

  public boolean isUp() {
    return ready.get();
  }
}
