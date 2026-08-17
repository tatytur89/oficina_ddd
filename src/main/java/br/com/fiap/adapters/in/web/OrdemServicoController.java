package br.com.fiap.adapters.in.web;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.OrdemServicoUseCase;
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
@RequestMapping("/api/v1/os")
@Tag(
    name = "Ordens de Serviço",
    description = "Endpoints para gestão completa do fluxo de ordens de serviço"
)
public class OrdemServicoController {

    private final OrdemServicoUseCase osUseCase;

    public OrdemServicoController(OrdemServicoUseCase osUseCase) {
        this.osUseCase = osUseCase;
    }

    @Operation(
        summary = "Criar uma nova Ordem de Serviço",
        description = """
            Cria uma nova OS no sistema vinculando cliente, veículo, serviços e peças.
            
            **Fluxo inicial:**
            - A OS é criada com status `RECEBIDA`
            - Serviços e peças podem ser adicionados随后
            - O orçamento é calculado automaticamente
            
            **Máquina de estados:**
            ```
            RECEBIDA → EM_ANDAMENTO → AGUARDANDO_APROVACAO → APROVADA → EM_EXECUCAO → CONCLUIDA → ENTREGUE
            ```
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "OS criada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrdemServicoResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                            "id": 1,
                            "clienteId": 1,
                            "veiculoId": 1,
                            "status": "RECEBIDA",
                            "dataAbertura": "2026-08-12T10:30:00",
                            "dataPrevistaEntrega": "2026-08-20T18:00:00",
                            "observacoes": "Cliente relata barulho no freio",
                            "valorServicos": 0.00,
                            "valorPecas": 0.00,
                            "valorTotal": 0.00,
                            "servicos": [],
                            "pecas": []
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
    public ResponseEntity<OrdemServicoResponseDTO> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados para criação da OS",
                required = true
            )
            @Valid @RequestBody OrdemServicoRequestDTO requestDTO) {
        
        OrdemServico osDomain = new OrdemServico(
            null,
            requestDTO.getClienteId(),
            requestDTO.getVeiculoId(),
            null,
            null,
            requestDTO.getDataPrevistaEntrega(),
            null,
            requestDTO.getObservacoes(),
            null,
            null,
            null,
            null,
            null
        );

        OrdemServico osCriada = osUseCase.criarOS(osDomain);
        OrdemServicoResponseDTO responseDTO = new OrdemServicoResponseDTO(osCriada);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @Operation(
        summary = "Listar todas as OS (administrativo)",
        description = "Retorna uma lista com todas as ordens de serviço do sistema. Uso administrativo."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de OS retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrdemServicoResponseDTO.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<OrdemServicoResponseDTO>> listarTodas() {
        List<OrdemServico> osList = osUseCase.listarTodas();

        List<OrdemServicoResponseDTO> responseDTOs = osList.stream()
                .map(OrdemServicoResponseDTO::new)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(
        summary = "Buscar OS por ID",
        description = "Retorna os dados completos de uma OS específica, incluindo serviços e peças vinculadas."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OS encontrada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrdemServicoResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "OS não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrdemServicoResponseDTO> buscarPorId(
            @Parameter(
                description = "ID da Ordem de Serviço",
                example = "1",
                required = true
            )
            @PathVariable Long id) {
        
        return osUseCase.buscarPorId(id)
                .map(os -> ResponseEntity.ok(new OrdemServicoResponseDTO(os)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Listar OS por cliente",
        description = "Retorna todas as OS de um cliente específico, ordenadas por data de abertura."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de OS do cliente retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrdemServicoResponseDTO.class)
            )
        )
    })
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<OrdemServicoResponseDTO>> listarPorCliente(
            @Parameter(
                description = "ID do cliente",
                example = "1",
                required = true
            )
            @PathVariable Long clienteId) {
        
        List<OrdemServico> osList = osUseCase.listarPorCliente(clienteId);

        List<OrdemServicoResponseDTO> responseDTOs = osList.stream()
                .map(OrdemServicoResponseDTO::new)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(
        summary = "Listar OS por status",
        description = """
            Retorna todas as OS com um status específico.
            
            **Status disponíveis:**
            - RECEBIDA - OS acabou de ser criada
            - EM_ANDAMENTO - OS em análise/andamento
            - AGUARDANDO_APROVACAO - Orçamento enviado para aprovação
            - APROVADA - Cliente aprovou o orçamento
            - EM_EXECUCAO - Serviço em execução
            - CONCLUIDA - Serviço concluído
            - ENTREGUE - Veículo entregue ao cliente
            - CANCELADA - OS cancelada
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de OS retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrdemServicoResponseDTO.class)
            )
        )
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrdemServicoResponseDTO>> listarPorStatus(
            @Parameter(
                description = "Status da OS",
                example = "EM_EXECUCAO",
                required = true
            )
            @PathVariable StatusOS status) {
        
        List<OrdemServico> osList = osUseCase.listarPorStatus(status);

        List<OrdemServicoResponseDTO> responseDTOs = osList.stream()
                .map(OrdemServicoResponseDTO::new)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(
        summary = "Listar OS por período",
        description = """
            Retorna todas as OS abertas em um período específico.
            
            **Formato de data:** ISO 8601 (`AAAA-MM-DDTHH:mm:ss`)
            
            **Exemplo:** `2026-08-01T00:00:00` até `2026-08-31T23:59:59`
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de OS retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrdemServicoResponseDTO.class)
            )
        )
    })
    @GetMapping("/periodo")
    public ResponseEntity<List<OrdemServicoResponseDTO>> listarPorPeriodo(
            @Parameter(
                description = "Data/hora início do período (ISO 8601)",
                example = "2026-08-01T00:00:00",
                required = true
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            
            @Parameter(
                description = "Data/hora fim do período (ISO 8601)",
                example = "2026-08-31T23:59:59",
                required = true
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        
        List<OrdemServico> osList = osUseCase.listarPorPeriodo(inicio, fim);

        List<OrdemServicoResponseDTO> responseDTOs = osList.stream()
                .map(OrdemServicoResponseDTO::new)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(
        summary = "Enviar orçamento para aprovação",
        description = """
            Transiciona a OS para o status `AGUARDANDO_APROVACAO`.
            
            **Regras:**
            - A OS deve estar com status `EM_ANDAMENTO`
            - Deve ter pelo menos um serviço vinculado
            - O orçamento é calculado automaticamente
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Orçamento enviado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrdemServicoResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                            "id": 1,
                            "status": "AGUARDANDO_APROVACAO",
                            "valorServicos": 250.00,
                            "valorPecas": 91.80,
                            "valorTotal": 341.80
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Transição de status inválida",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "OS não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @PatchMapping("/{id}/orcamento")
    public ResponseEntity<OrdemServicoResponseDTO> enviarOrcamento(
            @Parameter(
                description = "ID da Ordem de Serviço",
                example = "1",
                required = true
            )
            @PathVariable Long id) {
        
        OrdemServico os = osUseCase.enviarOrcamento(id);
        return ResponseEntity.ok(new OrdemServicoResponseDTO(os));
    }

    @Operation(
        summary = "Atualizar status da OS",
        description = """
            Atualiza o status da OS conforme a máquina de estados definida.
            
            **Transições válidas:**
            | Status Atual | Próximos Status Válidos |
            |---|---|
            | RECEBIDA | EM_ANDAMENTO, CANCELADA |
            | EM_ANDAMENTO | AGUARDANDO_APROVACAO, CANCELADA |
            | AGUARDANDO_APROVACAO | APROVADA, CANCELADA |
            | APROVADA | EM_EXECUCAO, CANCELADA |
            | EM_EXECUCAO | CONCLUIDA |
            | CONCLUIDA | ENTREGUE |
            | ENTREGUE | *(nenhum)* |
            | CANCELADA | *(nenhum)* |
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status atualizado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrdemServicoResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Transição de status inválida",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                            "timestamp": "2026-08-12T10:30:00",
                            "status": 400,
                            "error": "Bad Request",
                            "message": "Transição inválida: Entregue → Em Execução. Próximos status válidos: []",
                            "path": "/api/v1/os/1/status/EM_EXECUCAO"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "OS não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<OrdemServicoResponseDTO> atualizarStatus(
            @Parameter(
                description = "ID da Ordem de Serviço",
                example = "1",
                required = true
            )
            @PathVariable Long id,
            
            @Parameter(
                description = "Novo status da OS",
                example = "EM_EXECUCAO",
                required = true
            )
            @PathVariable StatusOS status) {
        
        OrdemServico os = osUseCase.atualizarStatus(id, status);
        return ResponseEntity.ok(new OrdemServicoResponseDTO(os));
    }

    @Operation(
        summary = "Acompanhar progresso da OS (público)",
        description = """
            Endpoint **público** (não requer autenticação) para o cliente acompanhar o progresso da OS.
            
            Retorna informações básicas do status, valores e previsão de entrega.
            
            **Uso:** Ideal para implementação de tracking online para clientes.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Progresso retornado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrdemServicoResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                            "id": 1,
                            "status": "EM_EXECUCAO",
                            "dataAbertura": "2026-08-12T10:30:00",
                            "dataPrevistaEntrega": "2026-08-20T18:00:00",
                            "valorTotal": 341.80
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "OS não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @GetMapping("/{id}/acompanhar")
    public ResponseEntity<OrdemServicoResponseDTO> acompanhar(
            @Parameter(
                description = "ID da Ordem de Serviço",
                example = "1",
                required = true
            )
            @PathVariable Long id) {
        
        return osUseCase.buscarPorId(id)
                .map(os -> ResponseEntity.ok(new OrdemServicoResponseDTO(os)))
                .orElse(ResponseEntity.notFound().build());
    }
}
