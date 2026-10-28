package br.com.equipe.mock.enderecos;

/** Endereço devolvido pelo mock, no mesmo formato que a eventos-api espera receber. */
public record Endereco(String cep, String logradouro, String bairro, String cidade, String uf) {}
