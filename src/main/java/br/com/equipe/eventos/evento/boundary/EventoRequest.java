package br.com.equipe.eventos.evento.boundary;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Corpo de {@code POST /eventos} e {@code PUT /eventos/{id}}, validado com {@code @Valid} no
 * recurso HTTP.
 */
public record EventoRequest(
    @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 120, message = "Nome deve ter no maximo 120 caracteres")
        String nome,
    @Size(max = 500, message = "Descricao deve ter no maximo 500 caracteres") String descricao,
    @NotBlank(message = "CEP e obrigatorio")
        @Pattern(regexp = "[0-9]{8}", message = "CEP deve conter exatamente 8 digitos")
        String cep,
    @NotNull(message = "Data e hora sao obrigatorias")
        @Future(message = "Data e hora devem estar no futuro")
        LocalDateTime dataHora,
    @NotNull(message = "Capacidade e obrigatoria")
        @Positive(message = "Capacidade deve ser positiva")
        Integer capacidade) {}
