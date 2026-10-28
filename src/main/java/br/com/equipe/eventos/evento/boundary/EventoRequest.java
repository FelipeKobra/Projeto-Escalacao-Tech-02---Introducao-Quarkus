package br.com.equipe.eventos.evento.boundary;

import java.time.LocalDateTime;

/**
 * Corpo de {@code POST /eventos} e {@code PUT /eventos/{id}}. As anotações de Bean Validation
 * entram nos componentes deste record (ver docs/divisao-do-trabalho.md).
 */
public record EventoRequest(
    String nome, String descricao, String cep, LocalDateTime dataHora, Integer capacidade) {}
