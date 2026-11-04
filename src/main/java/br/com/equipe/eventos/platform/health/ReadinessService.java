package br.com.equipe.eventos.platform.health;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class ReadinessService {

  private final AtomicBoolean ready = new AtomicBoolean(false);

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
