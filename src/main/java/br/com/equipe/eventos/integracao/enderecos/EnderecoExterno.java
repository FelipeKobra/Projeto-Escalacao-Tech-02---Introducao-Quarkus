package br.com.equipe.eventos.integracao.enderecos;

/** Contrato da resposta do serviço externo (mock-enderecos, GET /enderecos/{cep}). */
public record EnderecoExterno(
    String cep, String logradouro, String bairro, String cidade, String uf) {}
