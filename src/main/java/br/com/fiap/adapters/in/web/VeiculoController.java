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
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.adapters.in.web.DTO.Dados.Response;
import br.com.fiap.adapters.in.web.mapper.VeiculoMapper;
import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.ports.in.VeiculoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/veiculos")
@Tag(
    name = "Veículos",
    description = "Endpoints para gestão de veículos vinculados a clientes"
)
public class VeiculoController {

    private final VeiculoUseCase veiculoUseCase;

    public VeiculoController(VeiculoUseCase veiculoUseCase) {
        this.veiculoUseCase = veiculoUseCase;
    }

    @Operation(
        summary = "Cadastrar um novo veículo",
        description = """
            Cria um novo veículo no sistema e o vincula a um cliente.
            
            **Regras:**
            - A placa deve ser única no sistema
            - Formatos aceitos: ABC1234 (antigo) ou ABC1D23 (Mercosul)
            - O cliente proprietário deve estar cadastrado
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Veículo cadastrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                            "status": "success",
                            "message": "Veículo cadastrado com sucesso",
                            "dados": {
                                "id": 1,
                                "marca": "Toyota",
                                "modelo": "Corolla",
                                "ano": 2022,
                                "placa": "ABC1D23",
                                "clienteId": 1
                            }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(examples = @ExampleObject(
                value = "{\"status\": \"error\", \"message\": \"marca: A marca é obrigatória\", \"dados\": null}"))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cliente informado não encontrado",
            content = @Content(examples = @ExampleObject(
                value = "{\"status\": \"error\", \"message\": \"Cliente não encontrado com ID: 1\", \"dados\": null}"))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Já existe um veículo com a placa informada",
            content = @Content(examples = @ExampleObject(
                value = "{\"status\": \"error\", \"message\": \"Veículo com placa informada já existe.\", \"dados\": null}"))
        )
    })
    @PostMapping
    public ResponseEntity<Response<VeiculoResponseDTO>> cadastrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do veículo a ser cadastrado",
                required = true
            )
            @Valid @RequestBody VeiculoRequestDTO requestDTO) {

        Veiculo veiculoDomain = VeiculoMapper.toDomain(null, requestDTO);

        Veiculo veiculoSalvo = veiculoUseCase.cadastrarVeiculo(veiculoDomain);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new Response<>("success", "Veículo cadastrado com sucesso", VeiculoMapper.toResponse(veiculoSalvo)));
    }

    @Operation(
        summary = "Listar todos os veículos",
        description = "Retorna uma lista com todos os veículos cadastrados no sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de veículos retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<Response<List<VeiculoResponseDTO>>> listarTodos() {
        List<Veiculo> veiculos = veiculoUseCase.listarTodos();

        return ResponseEntity.ok(new Response<>("success", "Veículos listados com sucesso", VeiculoMapper.toResponseList(veiculos)));
    }

    @Operation(
        summary = "Listar veículos por cliente",
        description = "Retorna todos os veículos vinculados a um cliente específico."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de veículos do cliente retornada com sucesso")
    })
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<Response<List<VeiculoResponseDTO>>> listarPorCliente(
            @Parameter(
                description = "ID do cliente",
                example = "1",
                required = true
            )
            @PathVariable Long clienteId) {

        List<Veiculo> veiculos = veiculoUseCase.listarPorCliente(clienteId);

        return ResponseEntity.ok(new Response<>("success", "Veículos do cliente listados com sucesso", VeiculoMapper.toResponseList(veiculos)));
    }

    @Operation(
        summary = "Buscar veículo por placa",
        description = """
            Retorna os dados de um veículo específico pela placa.
            
            A busca é case-insensitive e remove caracteres especiais automaticamente.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Veículo encontrado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Nenhum veículo encontrado com a placa informada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Veículo não encontrado com a placa: ABC1D23\", \"dados\": null}")))
    })
    @GetMapping("/placa/{placa}")
    public ResponseEntity<Response<VeiculoResponseDTO>> buscarPorPlaca(
            @Parameter(
                description = "Placa do veículo (ABC1234 ou ABC1D23)",
                example = "ABC1D23",
                required = true
            )
            @PathVariable String placa) {

        Veiculo veiculo = veiculoUseCase.buscarPorPlaca(placa);

        return ResponseEntity.ok(new Response<>("success", "Veículo encontrado com sucesso", VeiculoMapper.toResponse(veiculo)));
    }

    @Operation(summary = "Atualizar um veículo existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Veículo atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"marca: A marca é obrigatória\", \"dados\": null}"))),
        @ApiResponse(responseCode = "404", description = "Veículo ou cliente informado não encontrado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Veículo não encontrado com ID: 1\", \"dados\": null}"))),
        @ApiResponse(responseCode = "409", description = "Já existe um veículo com a placa informada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Veículo com placa informada já existe.\", \"dados\": null}")))
    })
    @PutMapping("/{id}")
    public ResponseEntity<Response<VeiculoResponseDTO>> atualizar(
            @Parameter(description = "ID do veículo", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody VeiculoRequestDTO requestDTO) {

        Veiculo veiculoDomain = VeiculoMapper.toDomain(id, requestDTO);

        Veiculo veiculoAtualizado = veiculoUseCase.atualizarVeiculo(veiculoDomain);

        return ResponseEntity.ok(new Response<>("success", "Veículo atualizado com sucesso", VeiculoMapper.toResponse(veiculoAtualizado)));
    }

    @Operation(summary = "Excluir um veículo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Veículo excluído com sucesso"),
        @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Veículo não encontrado com ID: 1\", \"dados\": null}")))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> excluir(
            @Parameter(description = "ID do veículo", example = "1")
            @PathVariable Long id) {

        veiculoUseCase.excluirVeiculo(id);

        return ResponseEntity.ok(new Response<>("success", "Veículo excluído com sucesso", null));
    }
}
