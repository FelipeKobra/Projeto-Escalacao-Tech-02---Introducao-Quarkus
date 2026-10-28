package br.com.equipe.eventos.integracao.enderecos;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Consulta de endereço com tolerância a falhas: {@code @Retry} + {@code @Timeout} +
 * {@code @Fallback} (MicroProfile Fault Tolerance). Com o mock fora do ar a API responde de forma
 * controlada em vez de 500.
 */
@ApplicationScoped
public class EnderecoService {

  // TODO (Romulo): injetar EnderecoClient com @RestClient.

  public EnderecoExterno buscar(final String cep) {
    // TODO (Romulo): chamar o client; anotar com @Retry, @Timeout e @Fallback.
    throw new UnsupportedOperationException("TODO");
  }
}
