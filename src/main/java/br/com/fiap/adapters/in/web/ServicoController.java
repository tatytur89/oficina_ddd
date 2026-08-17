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

import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.TipoServico;
import br.com.fiap.ports.in.ServicoUseCase;
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
@RequestMapping("/api/v1/servicos")
@Tag(
    name = "Serviços",
    description = "Endpoints para gestão do catálogo de serviços de manutenção"
)
public class ServicoController {

    private final ServicoUseCase servicoUseCase;

    public ServicoController(ServicoUseCase servicoUseCase) {
        this.servicoUseCase = servicoUseCase;
    }

    @Operation(
        summary = "Cadastrar um novo serviço",
        description = """
            Cria um novo serviço de manutenção no catálogo.
            
            **Tipos de serviço disponíveis:**
            - REVISAO - Revisão geral
            - MANUTENCAO - Manutenção preventiva/corretiva
            - TROCA_PECA - Troca de peças
            - ALINHAMENTO - Alinhamento de direção
            - BALANCEAMENTO - Balanceamento de rodas
            - MECANICA_GERAL - Mecânica em geral
            - ELETRICA - Serviços elétricos
            - SUSPENSAO - Serviços de suspensão
            - FREIOS - Serviços de freios
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Serviço cadastrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ServicoResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                            "id": 1,
                            "nome": "Troca de Óleo",
                            "descricao": "Troca de óleo do motor 1.0 com filtro",
                            "preco": 150.00,
                            "tipo": "MANUTENCAO",
                            "tempoEstimadoMinutos": 60
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
        )
    })
    @PostMapping
    public ResponseEntity<ServicoResponseDTO> cadastrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do serviço a ser cadastrado",
                required = true
            )
            @Valid @RequestBody ServicoRequestDTO requestDTO) {
        
        Servico servicoDomain = new Servico(
            null,
            requestDTO.getNome(),
            requestDTO.getDescricao(),
            new Preco(requestDTO.getPreco()),
            TipoServico.valueOf(requestDTO.getTipo()),
            requestDTO.getTempoEstimadoMinutos()
        );

        Servico servicoSalvo = servicoUseCase.cadastrarServico(servicoDomain);
        ServicoResponseDTO responseDTO = new ServicoResponseDTO(servicoSalvo);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @Operation(
        summary = "Listar todos os serviços",
        description = "Retorna uma lista com todos os serviços disponíveis no catálogo."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de serviços retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ServicoResponseDTO.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<ServicoResponseDTO>> listarTodos() {
        List<Servico> servicos = servicoUseCase.listarTodos();

        List<ServicoResponseDTO> responseDTOs = servicos.stream()
                .map(ServicoResponseDTO::new)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(
        summary = "Buscar serviço por ID",
        description = "Retorna os dados completos de um serviço específico pelo seu ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Serviço encontrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ServicoResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Serviço não encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ServicoResponseDTO> buscarPorId(
            @Parameter(
                description = "ID do serviço",
                example = "1",
                required = true
            )
            @PathVariable Long id) {
        
        return servicoUseCase.buscarPorId(id)
                .map(servico -> ResponseEntity.ok(new ServicoResponseDTO(servico)))
                .orElse(ResponseEntity.notFound().build());
    }
}
