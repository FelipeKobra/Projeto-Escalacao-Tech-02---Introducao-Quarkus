package br.com.equipe.eventos.evento.boundary;

import br.com.equipe.eventos.evento.domain.EventoService;
import br.com.equipe.eventos.integracao.enderecos.EnderecoExterno;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/eventos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventoEnderecoResource {

  private final EventoService eventoService;

  public EventoEnderecoResource(EventoService eventoService) {
    this.eventoService = eventoService;
  }

  @GET
  @Path("/{id}/endereco")
  public EventoEnderecoResponse buscar(@PathParam("id") Long id){
    EnderecoExterno enderecoExterno = eventoService.getEndereco(id);
    return EventoEnderecoResponse.de(enderecoExterno);
  }

}
