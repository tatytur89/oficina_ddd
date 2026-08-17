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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
        summary = "Cadastrar um novo cliente",
        description = """
            Cria um novo cliente no sistema a partir de seus dados básicos.
            
            **Regras:**
            - O documento (CPF/CNPJ) deve ser único no sistema
            - CPF deve conter exatamente 11 dígitos
            - CNPJ deve conter exatamente 14 dígitos
            - E-mail deve ter formato válido
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Cliente cadastrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ClienteResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                            "id": 1,
                            "nome": "João da Silva",
                            "documento": "12345678909",
                            "email": "joao.silva@email.com",
                            "telefone": "11999998888"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos enviados na requisição",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Já existe um cliente com o CPF/CNPJ informado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> cadastrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do cliente a ser cadastrado",
                required = true,
                content = @Content(schema = @Schema(implementation = ClienteRequestDTO.class))
            )
            @Valid @RequestBody ClienteRequestDTO requestDTO) {
        
        Cliente clienteDomain = new Cliente(
                null,
                requestDTO.getNome(),
                requestDTO.getDocumento(),
                requestDTO.getEmail(),
                requestDTO.getTelefone()
        );

        Cliente clienteSalvo = clienteUseCase.cadastrarCliente(clienteDomain);
        ClienteResponseDTO responseDTO = new ClienteResponseDTO(clienteSalvo);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @Operation(
        summary = "Listar todos os clientes",
        description = "Retorna uma lista com todos os clientes cadastrados no sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de clientes retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ClienteResponseDTO.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> listarTodos() {
        List<Cliente> clientes = clienteUseCase.listarTodos();
        
        List<ClienteResponseDTO> responseDTOs = clientes.stream()
                .map(ClienteResponseDTO::new)
                .toList();
                
        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(
        summary = "Buscar cliente por CPF ou CNPJ",
        description = """
            Retorna os dados de um cliente específico com base no seu número de CPF ou CNPJ.
            
            O documento deve conter apenas números (11 para CPF ou 14 para CNPJ).
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cliente encontrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ClienteResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nenhum cliente encontrado com o documento informado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @GetMapping("/documento/{documento}")
    public ResponseEntity<ClienteResponseDTO> buscarPorDocumento(
            @Parameter(
                description = "Número do CPF ou CNPJ (apenas dígitos)",
                example = "12345678909",
                required = true
            )
            @PathVariable String documento) {
        
        return clienteUseCase.buscarPorDocumento(documento)
                .map(cliente -> ResponseEntity.ok(new ClienteResponseDTO(cliente)))
                .orElse(ResponseEntity.notFound().build());
    }
}
