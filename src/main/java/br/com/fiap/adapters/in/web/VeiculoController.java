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

import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.ports.in.VeiculoUseCase;
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
                schema = @Schema(implementation = VeiculoResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                            "id": 1,
                            "marca": "Toyota",
                            "modelo": "Corolla",
                            "ano": 2022,
                            "placa": "ABC1D23",
                            "clienteId": 1
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Já existe um veículo com a placa informada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<VeiculoResponseDTO> cadastrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do veículo a ser cadastrado",
                required = true
            )
            @Valid @RequestBody VeiculoRequestDTO requestDTO) {
        
        Veiculo veiculoDomain = new Veiculo(
            null,
            requestDTO.getMarca(),
            requestDTO.getModelo(),
            requestDTO.getAno(),
            requestDTO.getPlaca(),
            requestDTO.getClienteId()
        );

        Veiculo veiculoSalvo = veiculoUseCase.cadastrarVeiculo(veiculoDomain);
        VeiculoResponseDTO responseDTO = new VeiculoResponseDTO(veiculoSalvo);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @Operation(
        summary = "Listar todos os veículos",
        description = "Retorna uma lista com todos os veículos cadastrados no sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de veículos retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VeiculoResponseDTO.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<VeiculoResponseDTO>> listarTodos() {
        List<Veiculo> veiculos = veiculoUseCase.listarTodos();

        List<VeiculoResponseDTO> responseDTOs = veiculos.stream()
                .map(VeiculoResponseDTO::new)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(
        summary = "Listar veículos por cliente",
        description = "Retorna todos os veículos vinculados a um cliente específico."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de veículos do cliente retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VeiculoResponseDTO.class)
            )
        )
    })
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<VeiculoResponseDTO>> listarPorCliente(
            @Parameter(
                description = "ID do cliente",
                example = "1",
                required = true
            )
            @PathVariable Long clienteId) {
        
        List<Veiculo> veiculos = veiculoUseCase.listarPorCliente(clienteId);

        List<VeiculoResponseDTO> responseDTOs = veiculos.stream()
                .map(VeiculoResponseDTO::new)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(
        summary = "Buscar veículo por placa",
        description = """
            Retorna os dados de um veículo específico pela placa.
            
            A busca é case-insensitive e remove caracteres especiais automaticamente.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Veículo encontrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VeiculoResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nenhum veículo encontrado com a placa informada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @GetMapping("/placa/{placa}")
    public ResponseEntity<VeiculoResponseDTO> buscarPorPlaca(
            @Parameter(
                description = "Placa do veículo (ABC1234 ou ABC1D23)",
                example = "ABC1D23",
                required = true
            )
            @PathVariable String placa) {
        
        return veiculoUseCase.buscarPorPlaca(placa)
                .map(veiculo -> ResponseEntity.ok(new VeiculoResponseDTO(veiculo)))
                .orElse(ResponseEntity.notFound().build());
    }
}
