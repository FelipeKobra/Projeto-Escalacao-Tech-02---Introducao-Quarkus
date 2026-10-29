package br.com.equipe.eventos.evento.boundary;

import br.com.equipe.eventos.integracao.enderecos.EnderecoExterno;

public record EventoEnderecoResponse(
  String cep,
  String logradouro,
  String bairro,
  String cidade,
  String uf) {

  public static EventoEnderecoResponse de(EnderecoExterno enderecoExterno) {
    return new EventoEnderecoResponse(
      enderecoExterno.cep(),
      enderecoExterno.logradouro(),
      enderecoExterno.bairro(),
      enderecoExterno.cidade(),
      enderecoExterno.uf()
    );
  }
}
