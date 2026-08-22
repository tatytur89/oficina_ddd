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

import br.com.fiap.adapters.in.web.DTO.Dados.Response;
import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoCreateRequest;
import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoResponse;
import br.com.fiap.adapters.in.web.DTO.OrdemServico.TempoMedioExecucaoResponse;
import br.com.fiap.adapters.in.web.mapper.OrdemServicoMapper;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.OrdemServicoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ordens-servico")
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
            - Serviços e peças informados no corpo já são adicionados à OS
            - O orçamento é calculado automaticamente

            **Máquina de estados:**
            ```
            RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
            ```
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "OS criada com sucesso",
            content = @Content(examples = @ExampleObject(
                value = """
                    {
                        "status": "success",
                        "message": "OS criada com sucesso",
                        "dados": {
                            "id": 1,
                            "clienteId": 1,
                            "veiculoId": 1,
                            "status": "RECEBIDA",
                            "dataAbertura": "2026-08-12T10:30:00",
                            "dataPrevistaEntrega": "2026-08-20T18:00:00",
                            "observacoes": "Cliente relata barulho no freio",
                            "valorServicos": 150.00,
                            "valorPecas": 45.90,
                            "valorTotal": 195.90,
                            "servicos": [
                                {"servicoId": 1, "nomeServico": "Troca de Óleo", "quantidade": 1, "precoUnitario": 150.00, "valorTotal": 150.00}
                            ],
                            "pecas": [
                                {"pecaId": 1, "nomePeca": "Filtro de Óleo", "codigoPeca": "FIL001", "quantidade": 1, "precoUnitario": 45.90, "valorTotal": 45.90}
                            ]
                        }
                    }
                    """
            ))
        ),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"clienteId: O ID do cliente é obrigatório\", \"dados\": null}"))),
        @ApiResponse(responseCode = "404", description = "Cliente, veículo, serviço ou peça informado não encontrado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Cliente não encontrado com ID: 1\", \"dados\": null}")))
    })
    @PostMapping
    public ResponseEntity<Response<OrdemServicoResponse>> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados para criação da OS",
                required = true
            )
            @Valid @RequestBody OrdemServicoCreateRequest requestDTO) {

        OrdemServico osDomain = OrdemServicoMapper.toDomain(requestDTO);

        OrdemServico osCriada = osUseCase.criarOS(
                osDomain,
                OrdemServicoMapper.toItensServicos(requestDTO),
                OrdemServicoMapper.toItensPecas(requestDTO)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new Response<>("success", "OS criada com sucesso", OrdemServicoMapper.toResponse(osCriada)));
    }

    @Operation(
        summary = "Listar todas as OS (administrativo)",
        description = "Retorna uma lista com todas as ordens de serviço do sistema. Uso administrativo."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de OS retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<Response<List<OrdemServicoResponse>>> listarTodas() {
        List<OrdemServico> osList = osUseCase.listarTodas();

        return ResponseEntity.ok(new Response<>("success", "OS listadas com sucesso", OrdemServicoMapper.toResponseList(osList)));
    }

    @Operation(
        summary = "Buscar OS por ID",
        description = "Retorna os dados completos de uma OS específica, incluindo serviços e peças vinculadas."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OS encontrada com sucesso"),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"OS não encontrada com ID: 1\", \"dados\": null}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Response<OrdemServicoResponse>> buscarPorId(
            @Parameter(description = "ID da Ordem de Serviço", example = "1", required = true)
            @PathVariable Long id) {

        OrdemServico os = osUseCase.buscarPorId(id);

        return ResponseEntity.ok(new Response<>("success", "OS encontrada com sucesso", OrdemServicoMapper.toResponse(os)));
    }

    @Operation(
        summary = "Listar OS por cliente",
        description = "Retorna todas as OS de um cliente específico, ordenadas por data de abertura."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de OS do cliente retornada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Cliente não encontrado com ID: 1\", \"dados\": null}")))
    })
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<Response<List<OrdemServicoResponse>>> listarPorCliente(
            @Parameter(description = "ID do cliente", example = "1", required = true)
            @PathVariable Long clienteId) {

        List<OrdemServico> osList = osUseCase.listarPorCliente(clienteId);

        return ResponseEntity.ok(new Response<>("success", "OS do cliente listadas com sucesso", OrdemServicoMapper.toResponseList(osList)));
    }

    @Operation(
        summary = "Listar OS por status",
        description = """
            Retorna todas as OS com um status específico.

            **Status disponíveis:**
            - RECEBIDA - OS acabou de ser criada
            - EM_DIAGNOSTICO - OS em análise/diagnóstico
            - AGUARDANDO_APROVACAO - Orçamento enviado para aprovação
            - EM_EXECUCAO - Serviço em execução (baixa o estoque das peças vinculadas)
            - FINALIZADA - Serviço finalizado
            - ENTREGUE - Veículo entregue ao cliente
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de OS retornada com sucesso")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<Response<List<OrdemServicoResponse>>> listarPorStatus(
            @Parameter(description = "Status da OS", example = "EM_EXECUCAO", required = true)
            @PathVariable StatusOS status) {

        List<OrdemServico> osList = osUseCase.listarPorStatus(status);

        return ResponseEntity.ok(new Response<>("success", "OS listadas com sucesso", OrdemServicoMapper.toResponseList(osList)));
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
        @ApiResponse(responseCode = "200", description = "Lista de OS retornada com sucesso")
    })
    @GetMapping("/periodo")
    public ResponseEntity<Response<List<OrdemServicoResponse>>> listarPorPeriodo(
            @Parameter(description = "Data/hora início do período (ISO 8601)", example = "2026-08-01T00:00:00", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,

            @Parameter(description = "Data/hora fim do período (ISO 8601)", example = "2026-08-31T23:59:59", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {

        List<OrdemServico> osList = osUseCase.listarPorPeriodo(inicio, fim);

        return ResponseEntity.ok(new Response<>("success", "OS listadas com sucesso", OrdemServicoMapper.toResponseList(osList)));
    }

    @Operation(
        summary = "Enviar orçamento para aprovação",
        description = """
            Transiciona a OS para o status `AGUARDANDO_APROVACAO`.

            **Regras:**
            - A OS deve estar com status `EM_DIAGNOSTICO`
            - Deve ter pelo menos um serviço vinculado
            - O orçamento é calculado automaticamente
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orçamento enviado com sucesso", content = @Content(
            examples = @ExampleObject(value = """
                {
                    "status": "success",
                    "message": "Orçamento enviado com sucesso",
                    "dados": {
                        "id": 1,
                        "status": "AGUARDANDO_APROVACAO",
                        "valorServicos": 250.00,
                        "valorPecas": 91.80,
                        "valorTotal": 341.80
                    }
                }
                """))),
        @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"A OS deve estar Em Diagnóstico para enviar orçamento.\", \"dados\": null}"))),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"OS não encontrada com ID: 1\", \"dados\": null}")))
    })
    @PatchMapping("/{id}/orcamento")
    public ResponseEntity<Response<OrdemServicoResponse>> enviarOrcamento(
            @Parameter(description = "ID da Ordem de Serviço", example = "1", required = true)
            @PathVariable Long id) {

        OrdemServico os = osUseCase.enviarOrcamento(id);

        return ResponseEntity.ok(new Response<>("success", "Orçamento enviado com sucesso", OrdemServicoMapper.toResponse(os)));
    }

    @Operation(
        summary = "Atualizar status da OS",
        description = """
            Atualiza o status da OS conforme a máquina de estados definida.

            **Transições válidas:**
            | Status Atual | Próximo Status Válido |
            |---|---|
            | RECEBIDA | EM_DIAGNOSTICO |
            | EM_DIAGNOSTICO | AGUARDANDO_APROVACAO |
            | AGUARDANDO_APROVACAO | EM_EXECUCAO |
            | EM_EXECUCAO | FINALIZADA |
            | FINALIZADA | ENTREGUE |
            | ENTREGUE | *(nenhum)* |

            Ao transicionar para `EM_EXECUCAO`, o estoque de cada peça vinculada à OS é baixado automaticamente.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Transição de status inválida", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Transição inválida: Entregue → Em Execução. Próximos status válidos: []\", \"dados\": null}"))),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"OS não encontrada com ID: 1\", \"dados\": null}")))
    })
    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<Response<OrdemServicoResponse>> atualizarStatus(
            @Parameter(description = "ID da Ordem de Serviço", example = "1", required = true)
            @PathVariable Long id,

            @Parameter(description = "Novo status da OS", example = "EM_EXECUCAO", required = true)
            @PathVariable StatusOS status) {

        OrdemServico os = osUseCase.atualizarStatus(id, status);

        return ResponseEntity.ok(new Response<>("success", "Status atualizado com sucesso", OrdemServicoMapper.toResponse(os)));
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
        @ApiResponse(responseCode = "200", description = "Progresso retornado com sucesso", content = @Content(
            examples = @ExampleObject(value = """
                {
                    "status": "success",
                    "message": "Progresso retornado com sucesso",
                    "dados": {
                        "id": 1,
                        "status": "EM_EXECUCAO",
                        "dataAbertura": "2026-08-12T10:30:00",
                        "dataPrevistaEntrega": "2026-08-20T18:00:00",
                        "valorTotal": 341.80
                    }
                }
                """))),
        @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"OS não encontrada com ID: 1\", \"dados\": null}")))
    })
    @GetMapping("/{id}/acompanhar")
    public ResponseEntity<Response<OrdemServicoResponse>> acompanhar(
            @Parameter(description = "ID da Ordem de Serviço", example = "1", required = true)
            @PathVariable Long id) {

        OrdemServico os = osUseCase.buscarPorId(id);

        return ResponseEntity.ok(new Response<>("success", "Progresso retornado com sucesso", OrdemServicoMapper.toResponse(os)));
    }

    @Operation(
        summary = "Monitorar tempo médio de execução das OS",
        description = """
            Calcula o tempo médio (em minutos) decorrido entre a abertura e a conclusão das ordens de serviço já finalizadas (status `FINALIZADA` ou `ENTREGUE`).

            **Uso:** Indicador administrativo de eficiência do atendimento.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tempo médio calculado com sucesso", content = @Content(
            examples = @ExampleObject(value = """
                {
                    "status": "success",
                    "message": "Tempo médio de execução calculado com sucesso",
                    "dados": {
                        "tempoMedioMinutos": 182.5,
                        "quantidadeOSConsideradas": 12
                    }
                }
                """)))
    })
    @GetMapping("/metricas/tempo-medio-execucao")
    public ResponseEntity<Response<TempoMedioExecucaoResponse>> tempoMedioExecucao() {
        TempoMedioExecucaoResponse response = new TempoMedioExecucaoResponse(osUseCase.calcularTempoMedioExecucao());

        return ResponseEntity.ok(new Response<>("success", "Tempo médio de execução calculado com sucesso", response));
    }
}
