package br.com.fiap.adapters.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.ports.in.ClienteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "Endpoints para gestão de clientes")
public class ClienteController {

    private final ClienteUseCase clienteUseCase;
    
    public ClienteController(ClienteUseCase clienteUseCase) {
        this.clienteUseCase = clienteUseCase;
    }

@Operation(summary = "Cadastrar um novo cliente", description = "Cria um novo cliente a partir de seus dados básicos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos enviados na requisição")
    })
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> cadastrar(@Valid @RequestBody ClienteRequestDTO requestDTO) {
        // Conversão do DTO para a entidade de domínio
        // 1. Mapeia de DTO para Entidade de Domínio
        Cliente clienteDomain = new Cliente(
                null, // O ID será gerado pelo banco
                requestDTO.getNome(),
                requestDTO.getDocumento(),
                requestDTO.getEmail(),
                requestDTO.getTelefone()
        );

        // 2. Chama o caso de uso (Core)
        Cliente clienteSalvo = clienteUseCase.cadastrarCliente(clienteDomain);

        // 3. Mapeia a resposta do Domínio para DTO e retorna 201 Created
        ClienteResponseDTO responseDTO = new ClienteResponseDTO(clienteSalvo);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    // Endpoint para listar todos os clientes
    @Operation(summary = "Listar todos os clientes", description = "Retorna uma lista com todos os clientes cadastrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listarTodos() {
        List<Cliente> clientes = clienteUseCase.listarTodos();
        
        List<ClienteResponseDTO> responseDTOs = clientes.stream()
                .map(ClienteResponseDTO::new)
                .toList();
                
        return ResponseEntity.ok(responseDTOs);
    }

    // Endpoint para buscar um cliente pelo CPF ou CNPJ
    @Operation(summary = "Buscar cliente por documento", description = "Retorna os dados de um cliente específico com base no seu CPF ou CNPJ.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    @GetMapping("/documento/{documento}")
    public ResponseEntity<ClienteResponseDTO> buscarPorDocumento(@PathVariable String documento) {
        return clienteUseCase.buscarPorDocumento(documento)
                .map(cliente -> ResponseEntity.ok(new ClienteResponseDTO(cliente)))
                .orElse(ResponseEntity.notFound().build());
    }

}
