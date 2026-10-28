package br.com.equipe.eventos.evento.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Acesso a dados de {@link Evento}. O {@link PanacheRepository} já entrega {@code listAll}, {@code
 * findByIdOptional}, {@code persist}, {@code deleteById} etc.; consultas específicas do domínio
 * entram aqui como métodos novos.
 */
@ApplicationScoped
public class EventoRepository implements PanacheRepository<Evento> {}
