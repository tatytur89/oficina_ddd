package br.com.fiap.adapters.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.adapters.in.web.DTO.Cliente.ClienteCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Cliente.ClienteUpdateRequest;
import br.com.fiap.adapters.in.web.DTO.Cliente.ClienteResponse;
import br.com.fiap.adapters.in.web.DTO.Dados.Response;
import br.com.fiap.adapters.in.web.mapper.ClienteMapper;
import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.ports.in.ClienteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
        description = "Cria um novo cliente. O CPF/CNPJ informado deve ser único no sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Cliente cadastrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"nome: O nome é obrigatório\", \"dados\": null}"))),
        @ApiResponse(responseCode = "409", description = "Já existe cliente com o CPF/CNPJ informado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Cliente com CPF/CNPJ informado já existe.\", \"dados\": null}")))
    })
    @PostMapping
    public ResponseEntity<Response<ClienteResponse>> cadastrar(@Valid @RequestBody ClienteCreateRequest requestDTO) {

        Cliente cliente = ClienteMapper.toDomain(requestDTO);

        Cliente clienteSalvo = clienteUseCase.cadastrarCliente(cliente);

        return ResponseEntity.status(HttpStatus.CREATED).body(new Response<>("success", "Cliente cadastrado com sucesso",
                        ClienteMapper.toClienteResponse(clienteSalvo)
                ));
    }

    @Operation(summary = "Listar todos os clientes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Clientes listados com sucesso")
    })
    @GetMapping
    public ResponseEntity<Response<List<ClienteResponse>>> listarTodos() {

        List<ClienteResponse> clientes = ClienteMapper.toClienteResponseList(clienteUseCase.listarTodos());

        return ResponseEntity.ok(new Response<>("success", "Clientes listados com sucesso", clientes));
    }

    @Operation(summary = "Buscar cliente por CPF/CNPJ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente encontrado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Cliente não encontrado\", \"dados\": null}")))
    })
    @GetMapping("/documento")
    public ResponseEntity<Response<ClienteResponse>> buscarPorDocumento(
            @Parameter(description = "CPF ou CNPJ do cliente", example = "12345678909")
            @RequestParam String documento) {

        Cliente cliente = clienteUseCase.buscarPorDocumento(documento);

        return ResponseEntity.ok(new Response<>("success", "Cliente encontrado com sucesso",
                        ClienteMapper.toClienteResponse(cliente)));
    }

    @Operation(summary = "Atualizar um cliente existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"nome: O nome é obrigatório\", \"dados\": null}"))),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Cliente não encontrado com ID: 1\", \"dados\": null}"))),
        @ApiResponse(responseCode = "409", description = "Já existe cliente com o CPF/CNPJ informado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Cliente com CPF/CNPJ informado já existe.\", \"dados\": null}")))
    })
    @PutMapping("/{id}")
    public ResponseEntity<Response<ClienteResponse>> atualizarCliente(
            @Parameter(description = "ID do cliente", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ClienteUpdateRequest clienteRequest) {

        Cliente cliente = ClienteMapper.toDomain(id, clienteRequest);

        Cliente clienteAtualizado = clienteUseCase.atualizarCliente(cliente);

        return ResponseEntity.ok(new Response<>("success", "Cliente atualizado com sucesso",
                        ClienteMapper.toClienteResponse(clienteAtualizado)));
    }

    @Operation(summary = "Excluir um cliente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente excluído com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Cliente não encontrado com ID: 1\", \"dados\": null}")))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> excluirCliente(
            @Parameter(description = "ID do cliente", example = "1")
            @PathVariable Long id) {

        clienteUseCase.excluirCliente(id);

        return ResponseEntity.ok(new Response<>("success", "Cliente excluído com sucesso", null));
    }
}
