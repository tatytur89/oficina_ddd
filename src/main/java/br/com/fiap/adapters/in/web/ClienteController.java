package br.com.fiap.adapters.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.adapters.in.web.DTO.Dados.Response;
import br.com.fiap.adapters.in.web.mapper.ClienteMapper;
import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.ports.in.ClienteUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/clientes")
@Tag(
    name = "Clientes",
    description = "Endpoints para gestão de clientes da oficina"
)
public class ClienteController {

    private final ClienteUseCase clienteUseCase;
    
    public ClienteController(ClienteUseCase clienteUseCase) {
        this.clienteUseCase = clienteUseCase;
    }

    @PostMapping
    public ResponseEntity<Response<ClienteResponseDTO>> cadastrar(@Valid @RequestBody ClienteRequestDTO requestDTO) {

        Cliente cliente = ClienteMapper.toDomain(null, requestDTO);

        Cliente clienteSalvo = clienteUseCase.cadastrarCliente(cliente);

        return ResponseEntity.status(HttpStatus.CREATED).body(new Response<>("success","Cliente cadastrado com sucesso",
                        ClienteMapper.toResponse(clienteSalvo)
                ));
    }

    @GetMapping
    public ResponseEntity<Response<List<ClienteResponseDTO>>> listarTodos() {

        List<ClienteResponseDTO> clientes = ClienteMapper.toResponseList(clienteUseCase.listarTodos());

        return ResponseEntity.ok(new Response<>("success","Clientes listados com sucesso", clientes));
        
    }

    @GetMapping("/documento")
    public ResponseEntity<Response<ClienteResponseDTO>> buscarPorDocumento(@RequestParam String documento) {

        Cliente cliente = clienteUseCase.buscarPorDocumento(documento);

        return ResponseEntity.ok(new Response<ClienteResponseDTO>("success","Cliente encontrado com sucesso", 
        						ClienteMapper.toResponse(cliente)));
    }
    
    @PutMapping()
    public ResponseEntity<Response<ClienteResponseDTO>> atualizarCliente (@RequestParam Long id, @Valid @RequestBody ClienteRequestDTO clienteRequest) {
    	
    	Cliente cliente = ClienteMapper.toDomain(id, clienteRequest);

        Cliente clienteAtualizado = clienteUseCase.atualizarCliente(cliente);

        return ResponseEntity.ok(new Response<ClienteResponseDTO>("success","Cliente atualizado com sucesso",
        						ClienteMapper.toResponse(clienteAtualizado)));

    }
    
    @DeleteMapping
    public ResponseEntity<Response<Void>> excluirCliente(@RequestParam Long id) {

        clienteUseCase.excluirCliente(id);

        return ResponseEntity.ok(new Response<>("success","Cliente excluído com sucesso",null));
    }
}
