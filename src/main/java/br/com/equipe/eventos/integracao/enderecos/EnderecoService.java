package br.com.equipe.eventos.integracao.enderecos;

import io.smallrye.faulttolerance.api.ExponentialBackoff;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.temporal.ChronoUnit;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Consulta de endereço com tolerância a falhas: {@code @Retry} + {@code @Timeout} +
 * {@code @Fallback} (MicroProfile Fault Tolerance). Com o mock fora do ar a API responde de forma
 * controlada em vez de 500.
 */
@ApplicationScoped
public class EnderecoService {

  private final EnderecoClient enderecoClient;

  public EnderecoService(@RestClient EnderecoClient enderecoClient) {
    this.enderecoClient = enderecoClient;
  }

  @Retry(
      maxRetries = 2,
      jitter = 50,
      jitterDelayUnit = ChronoUnit.MILLIS,
      delay = 200L,
      delayUnit = ChronoUnit.MILLIS)
  @ExponentialBackoff(factor = 2, maxDelay = 1000, maxDelayUnit = ChronoUnit.MILLIS)
  @Timeout(value = 1000L, unit = ChronoUnit.MILLIS)
  @Fallback(fallbackMethod = "fallback")
  public EnderecoExterno buscar(final String cep) {
    return enderecoClient.buscar(cep);
  }

  public EnderecoExterno fallback(String cep) {
    return new EnderecoExterno(cep, "logradouro", "bairro", "cidade", "uf");
  }
}
