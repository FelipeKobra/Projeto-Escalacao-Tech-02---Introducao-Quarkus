package br.com.equipe.eventos.evento.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento com vagas limitadas. É a entidade JPA (tabela {@code evento}); o contrato HTTP fica nos
 * records de {@code boundary}, nunca nesta classe.
 *
 * <p>Lombok: {@code @Getter}/{@code @Setter} geram os acessores de cada campo e
 * {@code @NoArgsConstructor} gera o construtor vazio que o Hibernate exige. Não usamos
 * {@code @Data} porque o {@code equals}/{@code hashCode}/{@code toString} gerados por ele quebram
 * entidades JPA (identidade muda depois do persist e lazy loading dispara no toString).
 */
@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
public class Evento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(length = 500)
  private String descricao;

  /** CEP só com dígitos; o endereço completo vem do serviço externo (mock-enderecos). */
  @Column(nullable = false, length = 8)
  private String cep;

  @Column(name = "data_hora", nullable = false)
  private LocalDateTime dataHora;

  @Column(nullable = false)
  private int capacidade;

  @Column(nullable = false)
  private int inscritos;
}
